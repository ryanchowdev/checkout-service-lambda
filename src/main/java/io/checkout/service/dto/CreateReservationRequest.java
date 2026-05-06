/*
 * CreateReservationRequest.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  DTO for the POST /reservations request body
 */
package io.checkout.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateReservationRequest {

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("item_id")
    private String itemId;

    @JsonProperty("quantity")
    private Integer quantity;

    public CreateReservationRequest() {
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