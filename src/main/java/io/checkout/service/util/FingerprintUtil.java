/*
 * FingerprintUtil.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Create a deterministic hash of the request data
 *  Used for idempotent retries
 */
package io.checkout.service.util;

import io.checkout.service.dto.CreateOrderRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class FingerprintUtil {

    private FingerprintUtil() {
    }

    public static String fingerprint(CreateOrderRequest request) {
        // Convert request into a canonical string
        String canonical = String.join("|",
                nullSafe(request.getCustomerId()),
                nullSafe(request.getItemId()),
                request.getQuantity() == null ? "" : request.getQuantity().toString()
        );

        // Hash with SHA-256 and return a hex string
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute request fingerprint", e);
        }
    }

    // Helper function for handling null values
    private static String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }

    // Helper function to convert hash to hex string
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}