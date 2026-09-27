package com.rikkeibank.accountservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkeibank.accountservice.dto.*;
import com.rikkeibank.accountservice.entity.*;
import com.rikkeibank.accountservice.service.AccountService;
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

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @Test
    @DisplayName("GET /api/accounts by ADMIN should return 200")
    void testGetAllAccounts() throws Exception {
        Account acc = Account.builder()
                .id(1L)
                .accountNumber("RKB123456789")
                .balance(new BigDecimal("5000000.00"))
                .currency("VND")
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountService.getAllAccounts()).thenReturn(List.of(acc));

        mockMvc.perform(get("/api/accounts")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountNumber").value("RKB123456789"));
    }

    @Test
    @DisplayName("PUT /api/accounts/{accountNumber}/debit should perform debit")
    void testDebitEndpoint() throws Exception {
        BalanceOperationRequest req = BalanceOperationRequest.builder()
                .amount(new BigDecimal("1000000.00"))
                .transactionRef("TX-001")
                .build();

        Account acc = Account.builder()
                .id(1L)
                .accountNumber("RKB123456789")
                .balance(new BigDecimal("4000000.00"))
                .currency("VND")
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountService.debit(eq("RKB123456789"), any(BalanceOperationRequest.class))).thenReturn(acc);

        mockMvc.perform(put("/api/accounts/RKB123456789/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(4000000.00));
    }
}
