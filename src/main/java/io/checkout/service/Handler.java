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
import io.checkout.service.exception.ConflictException;
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
        this.orderService = new OrderService(orderRepository, idempotencyRepository);
    }

    // Main Lambda entry point: route incoming API Gateway requests
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String method = safe(request.getHttpMethod());
        String path = normalizePath(request.getPath());

        context.getLogger().log("Incoming request: method=" + method + ", path=" + path);

        try {
            if ("GET".equals(method) && "/health".equals(path)) {
                return jsonResponse(200, Map.of("status", "ok"));
            }

            if ("POST".equals(method) && "/orders".equals(path)) {
                return handleCreateOrder(request);
            }

            return jsonResponse(404, new ErrorResponse("Not Found", "No route matched " + method + " " + path));
        } catch (ValidationException e) {
            return jsonResponse(400, new ErrorResponse("Bad Request", e.getMessage()));
        } catch (ConflictException e) {
            return jsonResponse(409, new ErrorResponse("Conflict", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return jsonResponse(400, new ErrorResponse("Bad Request", e.getMessage()));
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

    private String safe(String value) {
        return value == null ? "" : value;
    }

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