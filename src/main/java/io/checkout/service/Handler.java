package io.checkout.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal starter handler.
 *
 * Supported routes for now:
 *   GET /health
 *
 * Future routes:
 *   POST /orders
 *   GET /orders/{orderId}
 */
public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String method = safe(request.getHttpMethod());
        String path = normalizePath(request.getPath());

        logRequest(context, method, path);

        try {
            if ("GET".equals(method) && "/health".equals(path)) {
                return okJson(Map.of("status", "ok"));
            }

            return jsonResponse(
                    404,
                    Map.of(
                            "error", "Not Found",
                            "message", "No route matched " + method + " " + path
                    )
            );
        } catch (Exception e) {
            context.getLogger().log("Unhandled exception: " + e.getMessage());
            return jsonResponse(
                    500,
                    Map.of(
                            "error", "Internal Server Error",
                            "message", "Unexpected error"
                    )
            );
        }
    }

    private void logRequest(Context context, String method, String path) {
        context.getLogger().log("Incoming request: method=" + method + ", path=" + path);
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

    private APIGatewayProxyResponseEvent okJson(Object body) {
        return jsonResponse(200, body);
    }

    private APIGatewayProxyResponseEvent jsonResponse(int statusCode, Object body) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.setHeaders(headers);

        try {
            response.setBody(OBJECT_MAPPER.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            response.setStatusCode(500);
            response.setBody("{\"error\":\"Internal Server Error\",\"message\":\"Failed to serialize response\"}");
        }

        return response;
    }
}