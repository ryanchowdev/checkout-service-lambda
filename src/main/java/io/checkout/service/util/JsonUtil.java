/*
 * JsonUtil.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Use Jackson for JSON conversion
 */
package io.checkout.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// JSON helper methods for request parsing and response serialization
public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    public static ObjectMapper mapper() {
        return OBJECT_MAPPER;
    }

    // Convert request JSON into a Java DTO
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON request body", e);
        }
    }

    // Convert a Java object into a JSON string
    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }
}