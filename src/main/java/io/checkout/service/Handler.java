/*
 * Handler.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Entry point for Lambda function
 */
package io.checkout.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.checkout.service.config.DynamoDbClientFactory;
import io.checkout.service.dto.CreateOrderRequest;
import io.checkout.service.dto.CreateOrderResponse;
import io.checkout.service.dto.ErrorResponse;
import io.checkout.service.dto.GetOrderResponse;
import io.checkout.service.exception.ConflictException;
import io.checkout.service.exception.NotFoundException;
import io.checkout.service.exception.ValidationException;
import io.checkout.service.repository.IdempotencyRepository;
import io.checkout.service.repository.OrderRepository;
import io.checkout.service.service.OrderService;
import io.checkout.service.util.JsonUtil;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.HashMap;
import java.util.Map;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final OrderService orderService;

    // Initialize DynamoDB repositories and the main order service
    public Handler() {
        DynamoDbClient dynamoDbClient = DynamoDbClientFactory.getClient();
        String ordersTableName = System.getenv("ORDERS_TABLE_NAME");
        String idempotencyTableName = System.getenv("IDEMPOTENCY_TABLE_NAME");

        OrderRepository orderRepository = new OrderRepository(dynamoDbClient, ordersTableName);
        IdempotencyRepository idempotencyRepository = new IdempotencyRepository(dynamoDbClient, idempotencyTableName);
        this.orderService = new OrderService(orderRepository, idempotencyRepository, dynamoDbClient);
    }

    // Main Lambda entry point: route incoming API Gateway requests
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String method = safe(request.getHttpMethod());
        String path = normalizePath(request.getPath());

        // Log all incoming requests
        context.getLogger().log("Incoming request: method=" + method + ", path=" + path);

        try {
            // Health check
            if ("GET".equals(method) && "/health".equals(path)) {
                return jsonResponse(200, Map.of("status", "ok"));
            }

            // POST /orders
            if ("POST".equals(method) && "/orders".equals(path)) {
                return handleCreateOrder(request);
            }

            // GET /orders/{orderId}
            if ("GET".equals(method) && path.startsWith("/orders/")) {
                return handleGetOrder(request);
            }

            // Invalid route
            return jsonResponse(404, new ErrorResponse("Not Found", "No route matched " + method + " " + path));
        // Bad request, failed validation
        } catch (ValidationException e) {
            return jsonResponse(400, new ErrorResponse("Bad Request", e.getMessage()));
        // Idempotency conflict
        } catch (ConflictException e) {
            return jsonResponse(409, new ErrorResponse("Conflict", e.getMessage()));
        // Missing order
        } catch (NotFoundException e) {
            return jsonResponse(404, new ErrorResponse("Not Found", e.getMessage()));
        // Bad request, illegal argument
        } catch (IllegalArgumentException e) {
            return jsonResponse(400, new ErrorResponse("Bad Request", e.getMessage()));
        // Other exception
        } catch (Exception e) {
            context.getLogger().log("Unhandled exception: " + e.getMessage());
            return jsonResponse(500, new ErrorResponse("Internal Server Error", "Unexpected error"));
        }
    }

    // Parse the create order request and handle ordering logic in OrderService
    private APIGatewayProxyResponseEvent handleCreateOrder(APIGatewayProxyRequestEvent request) {
        String idempotencyKey = header(request, "Idempotency-Key");
        CreateOrderRequest createOrderRequest = JsonUtil.fromJson(request.getBody(), CreateOrderRequest.class);
        CreateOrderResponse response = orderService.createOrder(idempotencyKey, createOrderRequest);
        return jsonResponse(201, response);
    }

    // Handle the get order request by loading the order from DynamoDB
    private APIGatewayProxyResponseEvent handleGetOrder(APIGatewayProxyRequestEvent request) {
        String orderId = extractOrderId(request);
        GetOrderResponse response = orderService.getOrder(orderId);
        return jsonResponse(200, response);
    }

    // Extract the orderId from API Gateway path parameters or the raw request path
    private String extractOrderId(APIGatewayProxyRequestEvent request) {
        if (request.getPathParameters() != null && request.getPathParameters().containsKey("orderId")) {
            return request.getPathParameters().get("orderId");
        }

        // Fallback: manually parse the order ID from the URL path
        String path = normalizePath(request.getPath());
        String prefix = "/orders/";
        if (path.startsWith(prefix) && path.length() > prefix.length()) {
            return path.substring(prefix.length());
        }

        return null;
    }

    // Read a request header
    private String header(APIGatewayProxyRequestEvent request, String name) {
        if (request.getHeaders() == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    // Helper function for string handling
    private String safe(String value) {
        return value == null ? "" : value;
    }

    // Helper function to normalize path strings
    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    // Convert a Java object into a JSON HTTP response for API Gateway
    private APIGatewayProxyResponseEvent jsonResponse(int statusCode, Object body) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.setHeaders(headers);
        response.setBody(JsonUtil.toJson(body));
        return response;
    }
}