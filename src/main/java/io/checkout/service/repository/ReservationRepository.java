/*
 * ReservationRepository.java
 *
 * Author:
 *  Ryan Chow
 * 
 * Description:
 *  DynamoDB access layer for reservations
 */
package io.checkout.service.repository;

// Provides access to the reservations table name for transactional writes
public class ReservationRepository {

    private final String tableName;

    public ReservationRepository(String tableName) {
        this.tableName = tableName;
    }

    // Used by ReservationService when building DynamoDB transaction items
    public String getTableName() {
        return tableName;
    }
}