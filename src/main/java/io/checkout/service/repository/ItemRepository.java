/*
 * ItemRepository.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  DynamoDB access layer for items
 */
package io.checkout.service.repository;

import io.checkout.service.model.Item;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ItemRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public ItemRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    // Insert a new item only if the itemId does not already exist
    public boolean putItemIfAbsent(Item item) {
        Map<String, AttributeValue> itemAttributes = new HashMap<>();
        itemAttributes.put("itemId", AttributeValue.fromS(item.getItemId()));
        itemAttributes.put("name", AttributeValue.fromS(item.getName()));
        itemAttributes.put("availableQuantity", AttributeValue.fromN(item.getAvailableQuantity().toString()));
        itemAttributes.put("createdAt", AttributeValue.fromS(item.getCreatedAt()));
        itemAttributes.put("updatedAt", AttributeValue.fromS(item.getUpdatedAt()));

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(itemAttributes)
                // Prevent overwriting existing inventory records
                .conditionExpression("attribute_not_exists(itemId)")
                .build();

        try {
            dynamoDbClient.putItem(request);
            return true;
        // DynamoDB throws this when the item already exists
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    // Read an inventory item by its primary key with strong consistency
    public Optional<Item> getItem(String itemId) {
        Map<String, AttributeValue> key = Map.of(
                "itemId", AttributeValue.fromS(itemId)
        );

        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                // Request the latest committed value from DynamoDB
                .consistentRead(true)
                .build());

        // Return empty when the item does not exist
        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }

        // Convert DynamoDB attributes into the internal Item model
        Map<String, AttributeValue> itemAttributes = response.item();

        Item item = new Item();
        item.setItemId(readString(itemAttributes, "itemId"));
        item.setName(readString(itemAttributes, "name"));
        item.setCreatedAt(readString(itemAttributes, "createdAt"));
        item.setUpdatedAt(readString(itemAttributes, "updatedAt"));

        if (itemAttributes.containsKey("availableQuantity") && itemAttributes.get("availableQuantity").n() != null) {
            item.setAvailableQuantity(Integer.parseInt(itemAttributes.get("availableQuantity").n()));
        }

        return Optional.of(item);
    }

    // Helper function to read a field
    private String readString(Map<String, AttributeValue> item, String field) {
        if (!item.containsKey(field) || item.get(field).s() == null) {
            return null;
        }
        return item.get(field).s();
    }

    // Expose the table name for transaction operations
    public String getTableName() {
        return tableName;
    }
}