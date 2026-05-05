/*
 * ItemService.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Contains the main logic for the system's inventory
 */
package io.checkout.service.service;

import io.checkout.service.dto.CreateItemRequest;
import io.checkout.service.dto.ItemResponse;
import io.checkout.service.exception.ConflictException;
import io.checkout.service.exception.NotFoundException;
import io.checkout.service.exception.ValidationException;
import io.checkout.service.model.Item;
import io.checkout.service.repository.ItemRepository;

import java.time.Instant;

public class ItemService {

    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    // Validate and create a new inventory item
    public ItemResponse createItem(CreateItemRequest request) {
        validateCreateItem(request);

        String now = Instant.now().toString();

        Item item = new Item(
                request.getItemId().trim(),
                request.getName().trim(),
                request.getAvailableQuantity(),
                now,
                now
        );

        // Use a conditional write so duplicate item IDs are rejected
        boolean created = itemRepository.putItemIfAbsent(item);

        // Convert duplicate item creation into a 409 Conflict response
        if (!created) {
            throw new ConflictException("Item already exists: " + request.getItemId());
        }

        return toResponse(item);
    }

    // Fetch an inventory item and return it in API response format
    public ItemResponse getItem(String itemId) {
        validateItemId(itemId);

        // Retrieve item, or return 404 if the requested item does not exist
        Item item = itemRepository.getItem(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));

        return toResponse(item);
    }

    // Convert the internal item model into the public API response DTO
    private ItemResponse toResponse(Item item) {
        return new ItemResponse(
                item.getItemId(),
                item.getName(),
                item.getAvailableQuantity(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    // Validate required fields before creating inventory
    private void validateCreateItem(CreateItemRequest request) {
        if (request == null) {
            throw new ValidationException("Request body is required");
        }
        if (request.getItemId() == null || request.getItemId().isBlank()) {
            throw new ValidationException("item_id is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ValidationException("name is required");
        }
        if (request.getAvailableQuantity() == null || request.getAvailableQuantity() < 0) {
            throw new ValidationException("available_quantity must be greater than or equal to 0");
        }
    }

    // Validate the itemId path parameter before querying DynamoDB
    private void validateItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            throw new ValidationException("itemId path parameter is required");
        }
    }
}