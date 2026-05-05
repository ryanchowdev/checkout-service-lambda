/*
 * Item.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Internal model representing an inventory item stored in DynamoDB
 */
package io.checkout.service.model;

public class Item {

    private String itemId;
    private String name;
    private Integer availableQuantity;
    private String createdAt;
    private String updatedAt;

    public Item() {
    }

    public Item(String itemId, String name, Integer availableQuantity, String createdAt, String updatedAt) {
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