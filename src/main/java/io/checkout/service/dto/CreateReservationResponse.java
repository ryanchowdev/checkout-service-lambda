/*
 * CreateReservationResponse.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  DTO returned after a reservation is successfully created
 */
package io.checkout.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateReservationResponse {

    @JsonProperty("reservation_id")
    private String reservationId;

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("item_id")
    private String itemId;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("status")
    private String status;

    public CreateReservationResponse() {
    }

    public CreateReservationResponse(String reservationId, String customerId, String itemId, Integer quantity, String status) {
        this.reservationId = reservationId;
        this.customerId = customerId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.status = status;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
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
}