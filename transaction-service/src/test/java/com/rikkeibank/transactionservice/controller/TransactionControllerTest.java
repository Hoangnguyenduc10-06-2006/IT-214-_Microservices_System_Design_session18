package com.rikkeibank.transactionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.transactionservice.dto.TransferRequest;
import com.rikkeibank.transactionservice.entity.Transaction;
import com.rikkeibank.transactionservice.entity.TransactionStatus;
import com.rikkeibank.transactionservice.entity.TransactionType;
import com.rikkeibank.transactionservice.service.TransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    @DisplayName("POST /api/transactions/transfer by CUSTOMER should return 201")
    void testTransferByCustomer() throws Exception {
        TransferRequest req = TransferRequest.builder()
                .sourceAccountNumber("RKB0000000001")
                .destinationAccountNumber("RKB0000000002")
                .amount(new BigDecimal("500000.00"))
                .currency("VND")
                .build();

        Transaction tx = Transaction.builder()
                .id(1L)
                .transactionRef("TX-TEST-001")
                .sourceAccountNumber("RKB0000000001")
                .destinationAccountNumber("RKB0000000002")
                .amount(new BigDecimal("500000.00"))
                .status(TransactionStatus.COMPLETED)
                .type(TransactionType.TRANSFER)
                .build();

        when(transactionService.executeTransfer(any(TransferRequest.class), eq(null))).thenReturn(tx);

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("X-User-Role", "CUSTOMER")
                        .header("X-User-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/transactions/today by TELLER returns only that teller's transactions")
    void testGetTodayTransactionsByTeller() throws Exception {
        Transaction tx = Transaction.builder()
                .id(1L)
                .tellerId(5L)
                .sourceAccountNumber("RKB0000000001")
                .destinationAccountNumber("RKB0000000002")
                .amount(new BigDecimal("200000.00"))
                .status(TransactionStatus.COMPLETED)
                .build();

        when(transactionService.getTodayTransactions(5L)).thenReturn(List.of(tx));

        mockMvc.perform(get("/api/transactions/today")
                        .header("X-User-Role", "TELLER")
                        .header("X-User-Id", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tellerId").value(5));
    }

    @Test
    @DisplayName("GET /api/transactions/today by CUSTOMER should return 403 Forbidden")
    void testGetTodayTransactionsByCustomerForbidden() throws Exception {
        mockMvc.perform(get("/api/transactions/today")
                        .header("X-User-Role", "CUSTOMER")
                        .header("X-User-Id", "10"))
                .andExpect(status().isForbidden());
    }
}
