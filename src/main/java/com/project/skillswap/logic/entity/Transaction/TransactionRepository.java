package com.project.skillswap.logic.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {

    /**
     * Finds a transaction by PayPal reference to avoid duplicate processing
     * @param paypalReference the PayPal transaction ID
     * @return Optional containing the transaction if found
     */
    Optional<Transaction> findByPaypalReference(String paypalReference);

    /**
     * Calculates total coins purchased today by a specific user
     * @param personId the person ID
     * @return sum of coins purchased today
     */
    @Query("SELECT COALESCE(SUM(t.skillcoinsAmount), 0) FROM Transaction t " +
            "WHERE t.person.id = :personId " +
            "AND t.type = 'PURCHASE' " +
            "AND t.status = 'COMPLETED' " +
            "AND DATE(t.transactionDate) = CURRENT_DATE")
    BigDecimal sumCoinsPurchasedToday(@Param("personId") Long personId);

    /**
     * Gets all transactions for a specific person ordered by date descending
     * @param personId the person ID
     * @return list of transactions
     */
    List<Transaction> findByPersonIdOrderByTransactionDateDesc(Long personId);

    /**
     * Gets all purchase transactions for a specific person
     * @param personId the person ID
     * @return list of purchase transactions
     */
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.person.id = :personId " +
            "AND t.type = 'PURCHASE' " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findPurchasesByPersonId(@Param("personId") Long personId);
}