/*
 * InsufficientInventoryException.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Exception thrown when there is insufficient inventory to process the order
 */
package io.checkout.service.exception;

public class InsufficientInventoryException extends RuntimeException {
    public InsufficientInventoryException(String message) {
        super(message);
    }
}
