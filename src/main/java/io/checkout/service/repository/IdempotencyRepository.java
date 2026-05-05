/*
 * IdempotencyRepository.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:\
 *  Repository for reading idempotency records from DynamoDB
 */
package io.checkout.service.repository;

import io.checkout.service.model.IdempotencyRecord;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.Map;
import java.util.Optional;

public class IdempotencyRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public IdempotencyRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    // Strongly read the idempotency record so retries see the latest committed state
    public Optional<IdempotencyRecord> getRecord(String idempotencyKey) {
        Map<String, AttributeValue> key = Map.of(
                "idempotencyKey", AttributeValue.fromS(idempotencyKey)
        );

        // Ensure strong consistency with consistentRead
        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .consistentRead(true)
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

    // Helper function for reading fields
    private String readString(Map<String, AttributeValue> item, String field) {
        if (!item.containsKey(field) || item.get(field).s() == null) {
            return null;
        }
        return item.get(field).s();
    }

    // Expose the table name so OrderService can use it in DynamoDB transactions
    public String getTableName() {
        return tableName;
    }
}