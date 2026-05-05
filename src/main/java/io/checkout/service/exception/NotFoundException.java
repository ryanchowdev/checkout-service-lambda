/*
 * NotFoundException.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Exception thrown when a requested order does not exist
 */
package io.checkout.service.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}