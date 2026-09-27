package com.rikkeibank.transactionservice.service;

import com.rikkeibank.transactionservice.client.AccountServiceClient;
import com.rikkeibank.transactionservice.dto.*;
import com.rikkeibank.transactionservice.entity.*;
import com.rikkeibank.transactionservice.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Transaction Service implementing the Saga Orchestrator Pattern.
 *
 * Transfer Flow (Saga Steps):
 * 1. Create PENDING transaction record
 * 2. DEBIT source account (via OpenFeign → account-service)
 * 3. CREDIT destination account (via OpenFeign → account-service)
 * 4. Mark transaction as COMPLETED
 * 5. Publish TRANSFER_COMPLETED event to Kafka
 *
 * Compensating Actions (on failure at step 3):
 * - COMPENSATE DEBIT on source account (refund)
 * - Mark transaction as COMPENSATED
 * - Publish TRANSFER_FAILED event to Kafka
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final TransactionEventProducer eventProducer;

    /**
     * Execute a money transfer using the Saga Orchestrator pattern.
     * Resilience4j CircuitBreaker protects against cascading failures to account-service.
     */
    @CircuitBreaker(name = "accountService", fallbackMethod = "transferFallback")
    public Transaction executeTransfer(TransferRequest request, Long tellerId) {
        String transactionRef = UUID.randomUUID().toString();

        // Validate: cannot transfer to same account
        if (request.getSourceAccountNumber().equals(request.getDestinationAccountNumber())) {
            throw new RuntimeException("Cannot transfer to the same account");
        }

        // Step 1: Create PENDING transaction record
        Transaction transaction = Transaction.builder()
                .transactionRef(transactionRef)
                .sourceAccountNumber(request.getSourceAccountNumber())
                .destinationAccountNumber(request.getDestinationAccountNumber())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .tellerId(tellerId)
                .build();
        transaction = transactionRepository.save(transaction);

        log.info("SAGA Step 1: Created PENDING transaction ref={}", transactionRef);

        BalanceOperationRequest balanceOp = BalanceOperationRequest.builder()
                .amount(request.getAmount())
                .transactionRef(transactionRef)
                .build();

        try {
            // Step 2: DEBIT source account
            log.info("SAGA Step 2: Debiting source account {}", request.getSourceAccountNumber());
            accountServiceClient.debit(request.getSourceAccountNumber(), balanceOp);

            try {
                // Step 3: CREDIT destination account
                log.info("SAGA Step 3: Crediting destination account {}", request.getDestinationAccountNumber());
                accountServiceClient.credit(request.getDestinationAccountNumber(), balanceOp);

            } catch (Exception creditException) {
                // Step 3 FAILED → COMPENSATE: Refund source account
                log.error("SAGA Step 3 FAILED: Credit failed, executing compensating debit. Error: {}",
                        creditException.getMessage());
                accountServiceClient.compensateDebit(request.getSourceAccountNumber(), balanceOp);

                transaction.setStatus(TransactionStatus.COMPENSATED);
                transactionRepository.save(transaction);

                // Publish TRANSFER_FAILED event
                publishEvent("TRANSFER_FAILED", transaction, "Credit failed, debit compensated");
                throw new RuntimeException("Transfer failed: could not credit destination account. " +
                        "Source account has been refunded. Ref: " + transactionRef);
            }

            // Step 4: Mark as COMPLETED
            transaction.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(transaction);

            log.info("SAGA Step 4: Transaction COMPLETED ref={}", transactionRef);

            // Step 5: Publish TRANSFER_COMPLETED event to Kafka
            publishEvent("TRANSFER_COMPLETED", transaction, "Transfer completed successfully");

            return transaction;

        } catch (RuntimeException e) {
            if (transaction.getStatus() == TransactionStatus.COMPENSATED) {
                throw e; // Already handled above
            }
            // Debit itself failed (insufficient balance, account frozen, etc.)
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
            publishEvent("TRANSFER_FAILED", transaction, "Debit failed: " + e.getMessage());
            throw new RuntimeException("Transfer failed: " + e.getMessage() + ". Ref: " + transactionRef);
        }
    }

    /**
     * Circuit Breaker fallback when account-service is unavailable.
     * Prevents cascading failure.
     */
    public Transaction transferFallback(TransferRequest request, Long tellerId, Exception ex) {
        log.error("CIRCUIT BREAKER OPEN: Account service unavailable. Error: {}", ex.getMessage());
        Transaction failed = Transaction.builder()
                .transactionRef(UUID.randomUUID().toString())
                .sourceAccountNumber(request.getSourceAccountNumber())
                .destinationAccountNumber(request.getDestinationAccountNumber())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.FAILED)
                .description("Service unavailable - Circuit Breaker OPEN")
                .tellerId(tellerId)
                .build();
        return transactionRepository.save(failed);
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /** TELLER: View today's transactions processed by this teller */
    public List<Transaction> getTodayTransactions(Long tellerId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return transactionRepository.findTodayByTellerId(tellerId, startOfDay);
    }

    /** CUSTOMER: View transaction history for a specific account */
    public List<Transaction> getTransactionsByAccount(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber);
    }

    private void publishEvent(String eventType, Transaction tx, String description) {
        try {
            TransactionEvent event = TransactionEvent.builder()
                    .eventType(eventType)
                    .transactionRef(tx.getTransactionRef())
                    .sourceAccountNumber(tx.getSourceAccountNumber())
                    .destinationAccountNumber(tx.getDestinationAccountNumber())
                    .amount(tx.getAmount())
                    .currency(tx.getCurrency())
                    .status(tx.getStatus().name())
                    .description(description)
                    .timestamp(LocalDateTime.now().toString())
                    .build();
            eventProducer.publishTransactionEvent(event);
        } catch (Exception e) {
            log.warn("Failed to publish event: {}", e.getMessage());
        }
    }
}
