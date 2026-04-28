/*
 * DynamoDbClientFactory.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  Create a DynamoDB client using the configured AWS region
 */
package io.checkout.service.config;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public final class DynamoDbClientFactory {

    private static final DynamoDbClient CLIENT = buildClient();

    private DynamoDbClientFactory() {
    }

    public static DynamoDbClient getClient() {
        return CLIENT;
    }

    private static DynamoDbClient buildClient() {
        String regionValue = System.getenv("AWS_REGION");
        if (regionValue == null || regionValue.isBlank()) {
            regionValue = System.getenv("AWS_DEFAULT_REGION");
        }
        if (regionValue == null || regionValue.isBlank()) {
            regionValue = "us-west-1";
        }

        return DynamoDbClient.builder()
                .region(Region.of(regionValue))
                .build();
    }
}