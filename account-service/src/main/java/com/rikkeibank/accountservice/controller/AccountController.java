package com.rikkeibank.accountservice.controller;

import com.rikkeibank.accountservice.dto.*;
import com.rikkeibank.accountservice.entity.*;
import com.rikkeibank.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // ==================== Account Type CRUD ====================

    @PostMapping("/api/account-types")
    public ResponseEntity<AccountType> createAccountType(@Valid @RequestBody AccountTypeRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validateAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccountType(request));
    }

    @GetMapping("/api/account-types")
    public ResponseEntity<List<AccountType>> getAllAccountTypes() {
        return ResponseEntity.ok(accountService.getAllAccountTypes());
    }

    @PutMapping("/api/account-types/{id}")
    public ResponseEntity<AccountType> updateAccountType(@PathVariable Long id,
            @Valid @RequestBody AccountTypeRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validateAdmin(role);
        return ResponseEntity.ok(accountService.updateAccountType(id, request));
    }

    @DeleteMapping("/api/account-types/{id}")
    public ResponseEntity<Void> deleteAccountType(@PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validateAdmin(role);
        accountService.deleteAccountType(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Account CRUD ====================

    @PostMapping("/api/accounts")
    public ResponseEntity<Account> createAccount(@Valid @RequestBody AccountRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validateAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    @GetMapping("/api/accounts")
    public ResponseEntity<List<Account>> getAllAccounts(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validateAdmin(role);
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/api/accounts/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @GetMapping("/api/accounts/number/{accountNumber}")
    public ResponseEntity<Account> getAccountByNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    @GetMapping("/api/accounts/customer/{customerId}")
    public ResponseEntity<List<Account>> getAccountsByCustomerId(@PathVariable Long customerId) {
        return ResponseEntity.ok(accountService.getAccountsByCustomerId(customerId));
    }

    @PutMapping("/api/accounts/{id}")
    public ResponseEntity<Account> updateAccount(@PathVariable Long id,
            @Valid @RequestBody AccountRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validateAdmin(role);
        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    @DeleteMapping("/api/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        validateAdmin(role);
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Balance Operations (Internal - called by transaction-service via Feign) ====================

    @PutMapping("/api/accounts/{accountNumber}/debit")
    public ResponseEntity<Account> debit(@PathVariable String accountNumber,
            @RequestBody BalanceOperationRequest request) {
        return ResponseEntity.ok(accountService.debit(accountNumber, request));
    }

    @PutMapping("/api/accounts/{accountNumber}/credit")
    public ResponseEntity<Account> credit(@PathVariable String accountNumber,
            @RequestBody BalanceOperationRequest request) {
        return ResponseEntity.ok(accountService.credit(accountNumber, request));
    }

    @PutMapping("/api/accounts/{accountNumber}/compensate-debit")
    public ResponseEntity<Account> compensateDebit(@PathVariable String accountNumber,
            @RequestBody BalanceOperationRequest request) {
        return ResponseEntity.ok(accountService.compensateDebit(accountNumber, request));
    }

    private void validateAdmin(String role) {
        if (!"ADMIN".equals(role)) throw new RuntimeException("Access denied. ADMIN role required.");
    }
}
