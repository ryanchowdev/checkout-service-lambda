/*
 * CreateOrderRequest.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Maps incoming JSON body sent to POST /orders into a Java object
 */
package io.checkout.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateOrderRequest {

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("item_id")
    private String itemId;

    @JsonProperty("quantity")
    private Integer quantity;

    public CreateOrderRequest() {
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
}