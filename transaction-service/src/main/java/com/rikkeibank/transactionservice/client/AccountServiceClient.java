package com.rikkeibank.transactionservice.client;

import com.rikkeibank.transactionservice.dto.BalanceOperationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * OpenFeign client for synchronous communication with account-service.
 * Uses service name registered in Eureka for load-balanced calls.
 */
@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @PutMapping("/api/accounts/{accountNumber}/debit")
    Map<String, Object> debit(@PathVariable("accountNumber") String accountNumber,
                              @RequestBody BalanceOperationRequest request);

    @PutMapping("/api/accounts/{accountNumber}/credit")
    Map<String, Object> credit(@PathVariable("accountNumber") String accountNumber,
                               @RequestBody BalanceOperationRequest request);

    @PutMapping("/api/accounts/{accountNumber}/compensate-debit")
    Map<String, Object> compensateDebit(@PathVariable("accountNumber") String accountNumber,
                                        @RequestBody BalanceOperationRequest request);

    @GetMapping("/api/accounts/number/{accountNumber}")
    Map<String, Object> getAccountByNumber(@PathVariable("accountNumber") String accountNumber);
}
