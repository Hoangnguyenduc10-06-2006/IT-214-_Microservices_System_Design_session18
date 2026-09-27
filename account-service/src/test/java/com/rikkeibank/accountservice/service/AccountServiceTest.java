package com.rikkeibank.accountservice.service;

import com.rikkeibank.accountservice.dto.*;
import com.rikkeibank.accountservice.entity.*;
import com.rikkeibank.accountservice.repository.AccountRepository;
import com.rikkeibank.accountservice.repository.AccountTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTypeRepository accountTypeRepository;

    @InjectMocks
    private AccountService accountService;

    private AccountType sampleType;
    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        sampleType = AccountType.builder()
                .id(1L)
                .typeName("SAVINGS")
                .description("Savings Account")
                .interestRate(5.5)
                .build();

        sampleAccount = Account.builder()
                .id(1L)
                .accountNumber("RKB1000000001")
                .customerId(10L)
                .accountType(sampleType)
                .balance(new BigDecimal("10000000.00"))
                .currency("VND")
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Create Account: Should save new account with generated account number")
    void testCreateAccount() {
        AccountRequest request = AccountRequest.builder()
                .customerId(10L)
                .accountTypeId(1L)
                .initialBalance(new BigDecimal("5000000.00"))
                .currency("VND")
                .build();

        when(accountTypeRepository.findById(1L)).thenReturn(Optional.of(sampleType));
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);

        Account created = accountService.createAccount(request);

        assertNotNull(created);
        assertEquals("RKB1000000001", created.getAccountNumber());
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Debit: Should decrease account balance when sufficient funds exist")
    void testDebitSuccess() {
        BalanceOperationRequest op = BalanceOperationRequest.builder()
                .amount(new BigDecimal("2000000.00"))
                .transactionRef("TX-12345")
                .build();

        when(accountRepository.findByAccountNumber("RKB1000000001")).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        Account debited = accountService.debit("RKB1000000001", op);

        assertNotNull(debited);
        assertEquals(new BigDecimal("8000000.00"), debited.getBalance());
    }

    @Test
    @DisplayName("Debit: Should throw exception on insufficient balance")
    void testDebitInsufficientBalance() {
        BalanceOperationRequest op = BalanceOperationRequest.builder()
                .amount(new BigDecimal("50000000.00")) // Exceeds 10,000,000
                .transactionRef("TX-12345")
                .build();

        when(accountRepository.findByAccountNumber("RKB1000000001")).thenReturn(Optional.of(sampleAccount));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.debit("RKB1000000001", op));
        assertTrue(ex.getMessage().contains("Insufficient balance"));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Credit: Should increase account balance")
    void testCreditSuccess() {
        BalanceOperationRequest op = BalanceOperationRequest.builder()
                .amount(new BigDecimal("3000000.00"))
                .transactionRef("TX-12345")
                .build();

        when(accountRepository.findByAccountNumber("RKB1000000001")).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        Account credited = accountService.credit("RKB1000000001", op);

        assertNotNull(credited);
        assertEquals(new BigDecimal("13000000.00"), credited.getBalance());
    }

    @Test
    @DisplayName("Compensate Debit (Saga Rollback): Should refund previously debited amount")
    void testCompensateDebit() {
        BalanceOperationRequest op = BalanceOperationRequest.builder()
                .amount(new BigDecimal("2000000.00"))
                .transactionRef("TX-12345")
                .build();

        when(accountRepository.findByAccountNumber("RKB1000000001")).thenReturn(Optional.of(sampleAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        Account compensated = accountService.compensateDebit("RKB1000000001", op);

        assertNotNull(compensated);
        assertEquals(new BigDecimal("12000000.00"), compensated.getBalance());
    }
}
