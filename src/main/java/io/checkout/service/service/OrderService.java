/*
 * OrderService.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Contains the main logic for the ordering system
 */
package io.checkout.service.service;

import io.checkout.service.dto.CreateOrderRequest;
import io.checkout.service.dto.CreateOrderResponse;
import io.checkout.service.exception.ConflictException;
import io.checkout.service.exception.ValidationException;
import io.checkout.service.model.Order;
import io.checkout.service.repository.IdempotencyRepository;
import io.checkout.service.repository.OrderRepository;
import io.checkout.service.util.FingerprintUtil;
import io.checkout.service.util.JsonUtil;

import java.time.Instant;
import java.util.UUID;

public class OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyRepository idempotencyRepository;

    public OrderService(OrderRepository orderRepository, IdempotencyRepository idempotencyRepository) {
        this.orderRepository = orderRepository;
        this.idempotencyRepository = idempotencyRepository;
    }

    // Create orders and handle idempotent retries
    public CreateOrderResponse createOrder(String idempotencyKey, CreateOrderRequest request) {
        // First validate request
        validate(idempotencyKey, request);

        // Generate request fingerprint
        String fingerprint = FingerprintUtil.fingerprint(request);
        String now = Instant.now().toString();

        // Try to claim idempotency key
        boolean claimed = idempotencyRepository.claimKey(idempotencyKey, fingerprint, now);

        // If unable to claim, then key already exists
        if (!claimed) {
            return idempotencyRepository.getRecord(idempotencyKey)
                    .map(existing -> {
                        // Different request: conflict
                        if (!fingerprint.equals(existing.getRequestFingerprint())) {
                            throw new ConflictException("Idempotency key was already used with a different request");
                        }

                        // Same request: replay stored response
                        if (existing.getResponseBody() != null) {
                            return JsonUtil.fromJson(existing.getResponseBody(), CreateOrderResponse.class);
                        }

                        // Conflict: Request in progress
                        throw new ConflictException("Request with this idempotency key is already in progress");
                    })
                    // Other conflict
                    .orElseThrow(() -> new ConflictException("Unable to resolve idempotency state"));
        }

        // Key was successfully claimed, so create a new order
        String orderId = UUID.randomUUID().toString();
        Order order = new Order(
                orderId,
                request.getCustomerId().trim(),
                request.getItemId().trim(),
                request.getQuantity(),
                "CREATED",
                now
        );

        // Save order to db
        orderRepository.putOrder(order);

        // Save completed response in idempotency table
        CreateOrderResponse response = new CreateOrderResponse(orderId, "CREATED");
        idempotencyRepository.saveCompletedResponse(idempotencyKey, 201, JsonUtil.toJson(response));

        return response;
    }

    // Validate required headers and request body fields before creating the order
    private void validate(String idempotencyKey, CreateOrderRequest request) {
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
}