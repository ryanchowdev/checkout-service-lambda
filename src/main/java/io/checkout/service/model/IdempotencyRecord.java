/*
 * IdempotencyRecord.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Stores request fingerprint and saved response so retries can be handled safely
 */
package io.checkout.service.model;

public class IdempotencyRecord {

    private String idempotencyKey;
    private String requestFingerprint;
    private Integer responseCode;
    private String responseBody;
    private String createdAt;

    public IdempotencyRecord() {
    }

    public IdempotencyRecord(String idempotencyKey, String requestFingerprint, Integer responseCode, String responseBody, String createdAt) {
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
        this.createdAt = createdAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public void setRequestFingerprint(String requestFingerprint) {
        this.requestFingerprint = requestFingerprint;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}