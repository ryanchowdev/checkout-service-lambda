/*
 * ItemResponse.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  DTO returned when an item is created or fetched
 */
package io.checkout.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemResponse {

    @JsonProperty("item_id")
    private String itemId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("available_quantity")
    private Integer availableQuantity;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    public ItemResponse() {
    }

    public ItemResponse(String itemId, String name, Integer availableQuantity, String createdAt, String updatedAt) {
        this.itemId = itemId;
        this.name = name;
        this.availableQuantity = availableQuantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}