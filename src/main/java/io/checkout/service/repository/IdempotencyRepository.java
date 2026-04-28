/*
 * IdempotencyRepository.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Persistence layer for idempotency data
 */
package io.checkout.service.repository;

import io.checkout.service.model.IdempotencyRecord;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class IdempotencyRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public IdempotencyRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    // Atomically claim an idempotency key
    // Only the first matching request should succeed
    public boolean claimKey(String idempotencyKey, String requestFingerprint, String createdAt) {
        // Request data
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("idempotencyKey", AttributeValue.fromS(idempotencyKey));
        item.put("requestFingerprint", AttributeValue.fromS(requestFingerprint));
        item.put("createdAt", AttributeValue.fromS(createdAt));

        // Try to insert new idempotency record
        // Ensure idempotencyKey does not already exist
        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .conditionExpression("attribute_not_exists(idempotencyKey)")
                .build();

        // On success: allow request to continue
        try {
            dynamoDbClient.putItem(request);
            return true;
        // On failure: Block duplicate request
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    // Load existing idempotency state to decide whether to replay or reject a retry
    public Optional<IdempotencyRecord> getRecord(String idempotencyKey) {
        Map<String, AttributeValue> key = Map.of(
                "idempotencyKey", AttributeValue.fromS(idempotencyKey)
        );

        // Check idempotency record by key
        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build());

        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }

        // Read response
        Map<String, AttributeValue> item = response.item();
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(readString(item, "idempotencyKey"));
        record.setRequestFingerprint(readString(item, "requestFingerprint"));
        record.setResponseBody(readString(item, "responseBody"));
        record.setCreatedAt(readString(item, "createdAt"));

        if (item.containsKey("responseCode") && item.get("responseCode").n() != null) {
            record.setResponseCode(Integer.parseInt(item.get("responseCode").n()));
        }

        return Optional.of(record);
    }

    // Save the completed response so future retries can return the same result
    public void saveCompletedResponse(String idempotencyKey, int responseCode, String responseBody) {
        Map<String, AttributeValue> key = Map.of(
                "idempotencyKey", AttributeValue.fromS(idempotencyKey)
        );

        // Store response code and response body
        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":responseCode", AttributeValue.fromN(Integer.toString(responseCode)));
        values.put(":responseBody", AttributeValue.fromS(responseBody));

        // Update idempotency record
        dynamoDbClient.updateItem(builder -> builder
                .tableName(tableName)
                .key(key)
                .updateExpression("SET responseCode = :responseCode, responseBody = :responseBody")
                .expressionAttributeValues(values)
        );
    }

    private String readString(Map<String, AttributeValue> item, String field) {
        if (!item.containsKey(field) || item.get(field).s() == null) {
            return null;
        }
        return item.get(field).s();
    }
}