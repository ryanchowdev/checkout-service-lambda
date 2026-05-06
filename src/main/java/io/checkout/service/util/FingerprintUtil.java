/*
 * FingerprintUtil.java
 *
 * Author:
 * Ryan Chow
 *
 * Description:
 * Create a deterministic hash of order request data.
 * Used to compare idempotent retries against the original request.
 */

package io.checkout.service.util;

import io.checkout.service.dto.CreateOrderRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class FingerprintUtil {

    private FingerprintUtil() {
    }

    // Generate a stable hash for order requests to support idempotent retries
    public static String fingerprint(CreateOrderRequest request) {
        // Use the business fields that define whether two order requests are identical
        String canonical = String.join("|",
                nullSafe(request.getCustomerId()),
                nullSafe(request.getItemId()),
                request.getQuantity() == null ? "" : request.getQuantity().toString()
        );

        // Hash the canonical request string with SHA-256
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

    // Helper function to convert hash bytes to a hex string
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}