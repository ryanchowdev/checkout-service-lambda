/*
 * CreateOrderResponse.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Response body for a successful request to POST /orders
*/
package io.checkout.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateOrderResponse {

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("status")
    private String status;

    public CreateOrderResponse() {
    }

    public CreateOrderResponse(String orderId, String status) {
        this.orderId = orderId;
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}