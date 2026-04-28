/*
 * OrderRepository.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Save a new order record into the orders DynamoDB table
 */
package io.checkout.service.repository;

import io.checkout.service.model.Order;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

public class OrderRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public OrderRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public void putOrder(Order order) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("orderId", AttributeValue.fromS(order.getOrderId()));
        item.put("customerId", AttributeValue.fromS(order.getCustomerId()));
        item.put("itemId", AttributeValue.fromS(order.getItemId()));
        item.put("quantity", AttributeValue.fromN(order.getQuantity().toString()));
        item.put("status", AttributeValue.fromS(order.getStatus()));
        item.put("createdAt", AttributeValue.fromS(order.getCreatedAt()));

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
    }
}