/*
 * ConflictException.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Exception thrown when the request conflicts with existing idempotency state
 */
package io.checkout.service.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}