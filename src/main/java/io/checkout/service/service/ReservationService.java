/*
 * ReservationService.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Contains the main logic for making reservations
 */
package io.checkout.service.service;

import io.checkout.service.dto.CreateReservationRequest;
import io.checkout.service.dto.CreateReservationResponse;
import io.checkout.service.exception.ConflictException;
import io.checkout.service.exception.ValidationException;
import io.checkout.service.model.IdempotencyRecord;
import io.checkout.service.repository.IdempotencyRepository;
import io.checkout.service.repository.ItemRepository;
import io.checkout.service.repository.ReservationRepository;
import io.checkout.service.util.FingerprintUtil;
import io.checkout.service.util.JsonUtil;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.Update;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReservationService {

    private final ItemRepository itemRepository;
    private final ReservationRepository reservationRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final DynamoDbClient dynamoDbClient;

    public ReservationService(
            ItemRepository itemRepository,
            ReservationRepository reservationRepository,
            IdempotencyRepository idempotencyRepository,
            DynamoDbClient dynamoDbClient
    ) {
        this.itemRepository = itemRepository;
        this.reservationRepository = reservationRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.dynamoDbClient = dynamoDbClient;
    }

    // Create a reservation while atomically decrementing inventory and saving idempotency state
    public CreateReservationResponse createReservation(String idempotencyKey, CreateReservationRequest request) {
        validateCreateReservation(idempotencyKey, request);

        // Fingerprint identifies whether a retry has the same request body
        String fingerprint = FingerprintUtil.fingerprint(request);
        String now = Instant.now().toString();
        String reservationId = UUID.randomUUID().toString();

        // Build the response before the transaction so it can be saved for idempotent replay
        CreateReservationResponse response = new CreateReservationResponse(
                reservationId,
                request.getCustomerId().trim(),
                request.getItemId().trim(),
                request.getQuantity(),
                "RESERVED"
        );

        String responseJson = JsonUtil.toJson(response);

        // Run the inventory decrement, reservation insert, and idempotency insert as one atomic operation
        try {
            createReservationTransaction(
                    reservationId,
                    request.getCustomerId().trim(),
                    request.getItemId().trim(),
                    request.getQuantity(),
                    idempotencyKey,
                    fingerprint,
                    responseJson,
                    now
            );

            return response;
        // Handle transaction failures, return appropriate API errors
        } catch (TransactionCanceledException e) {
            if (wasIdempotencyConflict(e)) {
                return handleExistingIdempotencyRecord(idempotencyKey, fingerprint);
            }

            if (wasInventoryConflict(e)) {
                throw new ConflictException("Insufficient inventory");
            }

            throw e;
        }
    }

    // Build and run the DynamoDB transaction
    private void createReservationTransaction(
            String reservationId,
            String customerId,
            String itemId,
            Integer quantity,
            String idempotencyKey,
            String requestFingerprint,
            String responseJson,
            String createdAt
    ) {
        Map<String, AttributeValue> itemKey = Map.of(
                "itemId", AttributeValue.fromS(itemId)
        );

        Map<String, AttributeValue> updateValues = new HashMap<>();
        updateValues.put(":quantity", AttributeValue.fromN(quantity.toString()));
        updateValues.put(":updatedAt", AttributeValue.fromS(createdAt));

        // Atomically decrement inventory only if enough stock is available
        TransactWriteItem decrementInventory = TransactWriteItem.builder()
                .update(Update.builder()
                        .tableName(itemRepository.getTableName())
                        .key(itemKey)
                        .updateExpression("SET availableQuantity = availableQuantity - :quantity, updatedAt = :updatedAt")
                        // Prevent overselling by rejecting updates that would make stock negative
                        .conditionExpression("attribute_exists(itemId) AND availableQuantity >= :quantity")
                        .expressionAttributeValues(updateValues)
                        .build())
                .build();

        Map<String, AttributeValue> reservationItem = new HashMap<>();
        reservationItem.put("reservationId", AttributeValue.fromS(reservationId));
        reservationItem.put("customerId", AttributeValue.fromS(customerId));
        reservationItem.put("itemId", AttributeValue.fromS(itemId));
        reservationItem.put("quantity", AttributeValue.fromN(quantity.toString()));
        reservationItem.put("status", AttributeValue.fromS("RESERVED"));
        reservationItem.put("createdAt", AttributeValue.fromS(createdAt));

        // Store the reservation record as part of the same transaction
        TransactWriteItem putReservation = TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName(reservationRepository.getTableName())
                        .item(reservationItem)
                        // Prevent overwriting if a reservation ID already exists
                        .conditionExpression("attribute_not_exists(reservationId)")
                        .build())
                .build();

        Map<String, AttributeValue> idempotencyItem = new HashMap<>();
        idempotencyItem.put("idempotencyKey", AttributeValue.fromS(idempotencyKey));
        idempotencyItem.put("requestFingerprint", AttributeValue.fromS(requestFingerprint));
        idempotencyItem.put("responseCode", AttributeValue.fromN("201"));
        idempotencyItem.put("responseBody", AttributeValue.fromS(responseJson));
        idempotencyItem.put("createdAt", AttributeValue.fromS(createdAt));

        // Save the completed response so identical retries can be replayed safely
        TransactWriteItem putIdempotency = TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName(idempotencyRepository.getTableName())
                        .item(idempotencyItem)
                        // Ensure the idempotency key can only be claimed once
                        .conditionExpression("attribute_not_exists(idempotencyKey)")
                        .build())
                .build();

        // Execute all reservation transactions atomically
        dynamoDbClient.transactWriteItems(TransactWriteItemsRequest.builder()
                .transactItems(List.of(decrementInventory, putReservation, putIdempotency))
                .build());
    }

    // Replay a saved reservation response if this is a safe idempotent retry
    private CreateReservationResponse handleExistingIdempotencyRecord(String idempotencyKey, String fingerprint) {
        IdempotencyRecord existing = idempotencyRepository.getRecord(idempotencyKey)
                .orElseThrow(() -> new ConflictException("Unable to resolve idempotency state"));

        // Same key with different payload is unsafe and must be rejected
        if (!fingerprint.equals(existing.getRequestFingerprint())) {
            throw new ConflictException("Idempotency key was already used with a different request");
        }

        // Missing response body
        if (existing.getResponseBody() == null) {
            throw new ConflictException("Existing idempotency record is incomplete");
        }

        // Return the originally saved response
        return JsonUtil.fromJson(existing.getResponseBody(), CreateReservationResponse.class);
    }

    // Check whether the inventory condition failed during the transaction
    private boolean wasInventoryConflict(TransactionCanceledException e) {
        List<CancellationReason> reasons = e.cancellationReasons();
        if (reasons == null || reasons.isEmpty()) {
            return false;
        }

        // Check conditional inventory decrement
        CancellationReason inventoryReason = reasons.get(0);
        return inventoryReason != null
                && "ConditionalCheckFailed".equals(inventoryReason.code());
    }

    // Check whether the idempotency key was already used
    private boolean wasIdempotencyConflict(TransactionCanceledException e) {
        List<CancellationReason> reasons = e.cancellationReasons();
        if (reasons == null || reasons.size() < 3) {
            return false;
        }

        // Check idempotency record insert
        CancellationReason idempotencyReason = reasons.get(2);
        return idempotencyReason != null
                && "ConditionalCheckFailed".equals(idempotencyReason.code());
    }

    // Validate required headers and request fields before attempting the reservation
    private void validateCreateReservation(String idempotencyKey, CreateReservationRequest request) {
        // Require Idempotency-Key
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationException("Missing required header: Idempotency-Key");
        }
        // Require non-empty request body
        if (request == null) {
            throw new ValidationException("Request body is required");
        }
        // Require customer_id
        if (request.getCustomerId() == null || request.getCustomerId().isBlank()) {
            throw new ValidationException("customer_id is required");
        }
        // Require item_id
        if (request.getItemId() == null || request.getItemId().isBlank()) {
            throw new ValidationException("item_id is required");
        }
        // Require positive quantity (> 0)
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new ValidationException("quantity must be greater than 0");
        }
    }
}