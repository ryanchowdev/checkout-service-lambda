/*
 * ValidationException.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Exception thrown when the client request is missing required data or has invalid values
 */
package io.checkout.service.exception;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}