package com.rikkeibank.transactionservice.repository;

import com.rikkeibank.transactionservice.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionRef(String transactionRef);

    List<Transaction> findBySourceAccountNumberOrDestinationAccountNumber(
            String sourceAccount, String destinationAccount);

    /** TELLER: view today's transactions they processed */
    @Query("SELECT t FROM Transaction t WHERE t.tellerId = :tellerId AND t.createdAt >= :startOfDay")
    List<Transaction> findTodayByTellerId(@Param("tellerId") Long tellerId,
                                          @Param("startOfDay") LocalDateTime startOfDay);

    /** All transactions for a specific account number (as source or destination) */
    @Query("SELECT t FROM Transaction t WHERE t.sourceAccountNumber = :accNum OR t.destinationAccountNumber = :accNum ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountNumber(@Param("accNum") String accountNumber);
}
