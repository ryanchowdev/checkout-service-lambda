/*
 * GetOrderResponse.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  DTO returned when fetching an existing order by ID
 */
package io.checkout.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetOrderResponse {

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("item_id")
    private String itemId;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("status")
    private String status;

    @JsonProperty("created_at")
    private String createdAt;

    public GetOrderResponse() {
    }

    public GetOrderResponse(String orderId, String customerId, String itemId, Integer quantity, String status, String createdAt) {
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