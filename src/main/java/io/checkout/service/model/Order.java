/*
 * Order.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Internal model representing an order record stored in DynamoDB
 */
package io.checkout.service.model;

public class Order {

    private String orderId;
    private String customerId;
    private String itemId;
    private Integer quantity;
    private String status;
    private String createdAt;

    public Order() {
    }

    public Order(String orderId, String customerId, String itemId, Integer quantity, String status, String createdAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}