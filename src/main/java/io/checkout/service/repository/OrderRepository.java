/*
 * OrderRepository.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Repository for reading order records from DynamoDB
 */
package io.checkout.service.repository;

import io.checkout.service.model.Order;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.Map;
import java.util.Optional;

public class OrderRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public OrderRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    // Strongly read an order by primary key so recently committed data is returned
    public Optional<Order> getOrder(String orderId) {
        Map<String, AttributeValue> key = Map.of(
                "orderId", AttributeValue.fromS(orderId)
        );

        // Ensure strong consistency with consistentRead
        GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .consistentRead(true)
                .build());

        // Return empty if no order exists with this ID
        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }

        // Read response
        Map<String, AttributeValue> item = response.item();
        Order order = new Order();
        order.setOrderId(readString(item, "orderId"));
        order.setCustomerId(readString(item, "customerId"));
        order.setItemId(readString(item, "itemId"));
        order.setStatus(readString(item, "status"));
        order.setCreatedAt(readString(item, "createdAt"));

        if (item.containsKey("quantity") && item.get("quantity").n() != null) {
            order.setQuantity(Integer.parseInt(item.get("quantity").n()));
        }

        return Optional.of(order);
    }

    // Helper function to read fields
    private String readString(Map<String, AttributeValue> item, String field) {
        if (!item.containsKey(field) || item.get(field).s() == null) {
            return null;
        }
        return item.get(field).s();
    }

    // Expose the table name for transaction-based writes in OrderService
    public String getTableName() {
        return tableName;
    }
}