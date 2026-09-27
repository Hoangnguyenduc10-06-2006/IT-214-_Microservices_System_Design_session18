package com.rikkeibank.accountservice.service;

import com.rikkeibank.accountservice.dto.*;
import com.rikkeibank.accountservice.entity.*;
import com.rikkeibank.accountservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Account service with balance operations for Saga pattern and Cache-Aside caching.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountTypeRepository accountTypeRepository;

    // ==================== Account Type CRUD ====================

    public AccountType createAccountType(AccountTypeRequest request) {
        AccountType type = AccountType.builder()
                .typeName(request.getTypeName())
                .description(request.getDescription())
                .interestRate(request.getInterestRate())
                .build();
        return accountTypeRepository.save(type);
    }

    public List<AccountType> getAllAccountTypes() {
        return accountTypeRepository.findAll();
    }

    public AccountType updateAccountType(Long id, AccountTypeRequest request) {
        AccountType type = accountTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account type not found: " + id));
        type.setTypeName(request.getTypeName());
        type.setDescription(request.getDescription());
        type.setInterestRate(request.getInterestRate());
        return accountTypeRepository.save(type);
    }

    public void deleteAccountType(Long id) {
        if (!accountTypeRepository.existsById(id))
            throw new RuntimeException("Account type not found: " + id);
        accountTypeRepository.deleteById(id);
    }

    // ==================== Account CRUD ====================

    public Account createAccount(AccountRequest request) {
        AccountType type = accountTypeRepository.findById(request.getAccountTypeId())
                .orElseThrow(() -> new RuntimeException("Account type not found: " + request.getAccountTypeId()));

        String accountNumber = generateAccountNumber();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .customerId(request.getCustomerId())
                .accountType(type)
                .balance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .status(AccountStatus.ACTIVE)
                .build();
        return accountRepository.save(account);
    }

    @Cacheable(value = "accounts", key = "#id")
    public Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found: " + id));
    }

    public Account getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public List<Account> getAccountsByCustomerId(Long customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    @CachePut(value = "accounts", key = "#id")
    public Account updateAccount(Long id, AccountRequest request) {
        Account account = getAccountById(id);
        AccountType type = accountTypeRepository.findById(request.getAccountTypeId())
                .orElseThrow(() -> new RuntimeException("Account type not found"));
        account.setCustomerId(request.getCustomerId());
        account.setAccountType(type);
        return accountRepository.save(account);
    }

    @CacheEvict(value = "accounts", key = "#id")
    public void deleteAccount(Long id) {
        if (!accountRepository.existsById(id))
            throw new RuntimeException("Account not found: " + id);
        accountRepository.deleteById(id);
    }

    // ==================== Balance Operations (Saga Pattern) ====================

    /**
     * Debit (subtract) from account. Used in Saga Step 2.
     * @throws RuntimeException if insufficient balance
     */
    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public Account debit(String accountNumber, BalanceOperationRequest request) {
        Account account = getAccountByNumber(accountNumber);

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Account " + accountNumber + " is not active");
        }

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance in account " + accountNumber);
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        log.info("DEBIT: Account {} debited {} (ref: {})",
                accountNumber, request.getAmount(), request.getTransactionRef());
        return accountRepository.save(account);
    }

    /**
     * Credit (add) to account. Used in Saga Step 3.
     */
    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public Account credit(String accountNumber, BalanceOperationRequest request) {
        Account account = getAccountByNumber(accountNumber);

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Account " + accountNumber + " is not active");
        }

        account.setBalance(account.getBalance().add(request.getAmount()));
        log.info("CREDIT: Account {} credited {} (ref: {})",
                accountNumber, request.getAmount(), request.getTransactionRef());
        return accountRepository.save(account);
    }

    /**
     * Compensating debit - reverse a debit operation (Saga Rollback).
     * Credits back the amount that was previously debited.
     */
    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public Account compensateDebit(String accountNumber, BalanceOperationRequest request) {
        Account account = getAccountByNumber(accountNumber);
        account.setBalance(account.getBalance().add(request.getAmount()));
        log.warn("COMPENSATE DEBIT: Account {} refunded {} (ref: {})",
                accountNumber, request.getAmount(), request.getTransactionRef());
        return accountRepository.save(account);
    }

    private String generateAccountNumber() {
        return "RKB" + System.currentTimeMillis() % 10000000000L;
    }
}
