/*
 * OrderService.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Contains the main logic for creating and retrieving orders with strong consistency
 */
package io.checkout.service.service;

import io.checkout.service.dto.CreateOrderRequest;
import io.checkout.service.dto.CreateOrderResponse;
import io.checkout.service.dto.GetOrderResponse;
import io.checkout.service.exception.ConflictException;
import io.checkout.service.exception.NotFoundException;
import io.checkout.service.exception.ValidationException;
import io.checkout.service.model.IdempotencyRecord;
import io.checkout.service.model.Order;
import io.checkout.service.repository.IdempotencyRepository;
import io.checkout.service.repository.OrderRepository;
import io.checkout.service.util.FingerprintUtil;
import io.checkout.service.util.JsonUtil;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final DynamoDbClient dynamoDbClient;

    public OrderService(
            OrderRepository orderRepository,
            IdempotencyRepository idempotencyRepository,
            DynamoDbClient dynamoDbClient
    ) {
        this.orderRepository = orderRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.dynamoDbClient = dynamoDbClient;
    }

    // Create orders and handle idempotent retries
    public CreateOrderResponse createOrder(String idempotencyKey, CreateOrderRequest request) {
        // First validate request
        validateCreateOrder(idempotencyKey, request);

        // Generate request fingerprint along with timestamp + ID
        // This allows us to distinguish identical retries from conflicting ones
        String fingerprint = FingerprintUtil.fingerprint(request);
        String now = Instant.now().toString();
        String orderId = UUID.randomUUID().toString();

        // Build final response before writing so it can be stored in the idempotency record transactionally
        CreateOrderResponse response = new CreateOrderResponse(orderId, "CREATED");
        String responseJson = JsonUtil.toJson(response);

        Order order = new Order(
                orderId,
                request.getCustomerId().trim(),
                request.getItemId().trim(),
                request.getQuantity(),
                "CREATED",
                now
        );

        // Write the order and saved response in one DynamoDB transaction
        // On success: return response
        try {
            createOrderTransaction(order, idempotencyKey, fingerprint, responseJson, now);
            return response;
        // On failure: Idempotency key already exists
        // Replay the saved response or reject conflicting payloads
        } catch (TransactionCanceledException e) {
            if (wasIdempotencyConflict(e)) {
                // Read existing idempotency record
                IdempotencyRecord existing = idempotencyRepository.getRecord(idempotencyKey)
                        .orElseThrow(() -> new ConflictException("Unable to resolve idempotency state"));

                // Compare request fingerprints
                if (!fingerprint.equals(existing.getRequestFingerprint())) {
                    throw new ConflictException("Idempotency key was already used with a different request");
                }

                // Incomplete idempotency record
                if (existing.getResponseBody() == null) {
                    throw new ConflictException("Existing idempotency record is incomplete");
                }

                // Replay response
                return JsonUtil.fromJson(existing.getResponseBody(), CreateOrderResponse.class);
            }

            throw e;
        }
    }

    // Fetch a previously created order by ID
    public GetOrderResponse getOrder(String orderId) {
        validateGetOrder(orderId);

        Order order = orderRepository.getOrder(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        return new GetOrderResponse(
                order.getOrderId(),
                order.getCustomerId(),
                order.getItemId(),
                order.getQuantity(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }

    // Use a cross-table DynamoDB transaction so order creation is atomic
    private void createOrderTransaction(
            Order order,
            String idempotencyKey,
            String requestFingerprint,
            String responseJson,
            String createdAt
    ) {
        // Order item
        Map<String, AttributeValue> orderItem = new HashMap<>();
        orderItem.put("orderId", AttributeValue.fromS(order.getOrderId()));
        orderItem.put("customerId", AttributeValue.fromS(order.getCustomerId()));
        orderItem.put("itemId", AttributeValue.fromS(order.getItemId()));
        orderItem.put("quantity", AttributeValue.fromN(order.getQuantity().toString()));
        orderItem.put("status", AttributeValue.fromS(order.getStatus()));
        orderItem.put("createdAt", AttributeValue.fromS(order.getCreatedAt()));

        // Idempotency item
        Map<String, AttributeValue> idempotencyItem = new HashMap<>();
        idempotencyItem.put("idempotencyKey", AttributeValue.fromS(idempotencyKey));
        idempotencyItem.put("requestFingerprint", AttributeValue.fromS(requestFingerprint));
        idempotencyItem.put("responseCode", AttributeValue.fromN("201"));
        idempotencyItem.put("responseBody", AttributeValue.fromS(responseJson));
        idempotencyItem.put("createdAt", AttributeValue.fromS(createdAt));

        // Transactional write entry for order
        // Require orderId
        TransactWriteItem putOrder = TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName(orderRepository.getTableName())
                        .item(orderItem)
                        .conditionExpression("attribute_not_exists(orderId)")
                        .build())
                .build();

        // Transactional write entry for idempotency
        // Require idempotencyKey
        TransactWriteItem putIdempotency = TransactWriteItem.builder()
                .put(Put.builder()
                        .tableName(idempotencyRepository.getTableName())
                        .item(idempotencyItem)
                        .conditionExpression("attribute_not_exists(idempotencyKey)")
                        .build())
                .build();

        // Execute atomic transaction
        dynamoDbClient.transactWriteItems(TransactWriteItemsRequest.builder()
                .transactItems(List.of(putOrder, putIdempotency))
                .build());
    }

    // Check whether the transaction failed because the idempotency key already existed
    private boolean wasIdempotencyConflict(TransactionCanceledException e) {
        List<CancellationReason> reasons = e.cancellationReasons();
        if (reasons == null || reasons.size() < 2) {
            return false;
        }

        CancellationReason idempotencyReason = reasons.get(1);
        return idempotencyReason != null
                && "ConditionalCheckFailed".equals(idempotencyReason.code());
    }

    // Validate required headers and request body fields before creating the order
    private void validateCreateOrder(String idempotencyKey, CreateOrderRequest request) {
        // Require idempotency key
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationException("Missing required header: Idempotency-Key");
        }
        // Require non-null request body
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
        // Require quantity > 0
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new ValidationException("quantity must be greater than 0");
        }
    }

    // Helper to ensure orderId is present before querying DynamoDB
    private void validateGetOrder(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new ValidationException("orderId path parameter is required");
        }
    }
}