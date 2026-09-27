package com.rikkeibank.transactionservice.controller;

import com.rikkeibank.transactionservice.dto.TransferRequest;
import com.rikkeibank.transactionservice.entity.Transaction;
import com.rikkeibank.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Transaction REST controller.
 * Handles money transfers, transaction history queries.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Initiate a money transfer (CUSTOMER or TELLER).
     * Executes the Saga Orchestrator pattern.
     */
    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        if (!"CUSTOMER".equals(role) && !"TELLER".equals(role) && !"ADMIN".equals(role)) {
            throw new RuntimeException("Access denied. CUSTOMER or TELLER role required.");
        }

        Long tellerId = "TELLER".equals(role) && userId != null ? Long.parseLong(userId) : null;
        Transaction result = transactionService.executeTransfer(request, tellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /** ADMIN: List all transactions */
    @GetMapping
    public ResponseEntity<List<Transaction>> getAllTransactions(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) throw new RuntimeException("Access denied. ADMIN role required.");
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    /**
     * TELLER: View today's transactions processed by this teller.
     * Only shows transactions where tellerId matches the authenticated user.
     */
    @GetMapping("/today")
    public ResponseEntity<List<Transaction>> getTodayTransactions(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        if (!"TELLER".equals(role)) throw new RuntimeException("Access denied. TELLER role required.");
        return ResponseEntity.ok(transactionService.getTodayTransactions(Long.parseLong(userId)));
    }

    /** CUSTOMER: View transaction history by account number (own account only) */
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<Transaction>> getTransactionsByAccount(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountNumber));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }
}
