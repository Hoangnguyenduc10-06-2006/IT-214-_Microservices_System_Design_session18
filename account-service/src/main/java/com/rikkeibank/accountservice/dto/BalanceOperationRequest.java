package com.rikkeibank.accountservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BalanceOperationRequest {
    private BigDecimal amount;
    private String transactionRef;
}
