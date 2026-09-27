package com.rikkeibank.accountservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AccountRequest {
    @NotNull(message = "Customer ID is required") private Long customerId;
    @NotNull(message = "Account type ID is required") private Long accountTypeId;
    private BigDecimal initialBalance;
    private String currency;
}
