package com.rikkeibank.transactionservice.service;

import com.rikkeibank.transactionservice.client.AccountServiceClient;
import com.rikkeibank.transactionservice.dto.BalanceOperationRequest;
import com.rikkeibank.transactionservice.dto.TransactionEvent;
import com.rikkeibank.transactionservice.dto.TransferRequest;
import com.rikkeibank.transactionservice.entity.Transaction;
import com.rikkeibank.transactionservice.entity.TransactionStatus;
import com.rikkeibank.transactionservice.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private TransactionEventProducer eventProducer;

    @InjectMocks
    private TransactionService transactionService;

    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        transferRequest = TransferRequest.builder()
                .sourceAccountNumber("RKB0000000001")
                .destinationAccountNumber("RKB0000000002")
                .amount(new BigDecimal("1000000.00"))
                .currency("VND")
                .description("Payment for invoice #123")
                .build();
    }

    @Test
    @DisplayName("SAGA Success: Should execute full transfer and mark COMPLETED")
    void testExecuteTransferSuccess() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
            Transaction t = i.getArgument(0);
            t.setId(1L);
            return t;
        });

        when(accountServiceClient.debit(eq("RKB0000000001"), any(BalanceOperationRequest.class)))
                .thenReturn(Map.of("status", "SUCCESS"));
        when(accountServiceClient.credit(eq("RKB0000000002"), any(BalanceOperationRequest.class)))
                .thenReturn(Map.of("status", "SUCCESS"));

        Transaction result = transactionService.executeTransfer(transferRequest, null);

        assertNotNull(result);
        assertEquals(TransactionStatus.COMPLETED, result.getStatus());

        // Verify Saga steps:
        verify(accountServiceClient, times(1)).debit(eq("RKB0000000001"), any(BalanceOperationRequest.class));
        verify(accountServiceClient, times(1)).credit(eq("RKB0000000002"), any(BalanceOperationRequest.class));
        verify(eventProducer, times(1)).publishTransactionEvent(any(TransactionEvent.class));
    }

    @Test
    @DisplayName("SAGA Rollback: Should execute compensating debit when credit step fails")
    void testExecuteTransferCompensateOnCreditFailure() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
            Transaction t = i.getArgument(0);
            t.setId(2L);
            return t;
        });

        // Step 2: Debit succeeds
        when(accountServiceClient.debit(eq("RKB0000000001"), any(BalanceOperationRequest.class)))
                .thenReturn(Map.of("status", "SUCCESS"));

        // Step 3: Credit FAILS (destination account frozen or network timeout)
        when(accountServiceClient.credit(eq("RKB0000000002"), any(BalanceOperationRequest.class)))
                .thenThrow(new RuntimeException("Destination account frozen"));

        // Compensating refund succeeds
        when(accountServiceClient.compensateDebit(eq("RKB0000000001"), any(BalanceOperationRequest.class)))
                .thenReturn(Map.of("status", "REFUNDED"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.executeTransfer(transferRequest, null));
        assertTrue(ex.getMessage().contains("could not credit destination account"));

        // Verify compensating action executed
        verify(accountServiceClient, times(1)).compensateDebit(eq("RKB0000000001"), any(BalanceOperationRequest.class));
    }

    @Test
    @DisplayName("SAGA Debit Failure: Should mark FAILED when source balance insufficient")
    void testExecuteTransferDebitFailure() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
            Transaction t = i.getArgument(0);
            t.setId(3L);
            return t;
        });

        when(accountServiceClient.debit(eq("RKB0000000001"), any(BalanceOperationRequest.class)))
                .thenThrow(new RuntimeException("Insufficient balance"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> transactionService.executeTransfer(transferRequest, null));
        assertTrue(ex.getMessage().contains("Insufficient balance"));

        // Credit should NEVER be called
        verify(accountServiceClient, never()).credit(anyString(), any());
        verify(accountServiceClient, never()).compensateDebit(anyString(), any());
    }

    @Test
    @DisplayName("Circuit Breaker Fallback: Should return FAILED transaction when account-service is down")
    void testCircuitBreakerFallback() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        Transaction fallbackTx = transactionService.transferFallback(
                transferRequest, null, new RuntimeException("Connection refused"));

        assertNotNull(fallbackTx);
        assertEquals(TransactionStatus.FAILED, fallbackTx.getStatus());
        assertTrue(fallbackTx.getDescription().contains("Circuit Breaker OPEN"));
    }
}
