/*
 * CreateItemRequest.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  DTO for creating an inventory item through POST /items
 */
package io.checkout.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateItemRequest {

    @JsonProperty("item_id")
    private String itemId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("available_quantity")
    private Integer availableQuantity;

    public CreateItemRequest() {
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
}