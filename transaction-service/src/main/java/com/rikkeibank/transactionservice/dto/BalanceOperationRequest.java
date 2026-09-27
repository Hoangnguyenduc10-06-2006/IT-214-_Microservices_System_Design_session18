package com.rikkeibank.transactionservice.dto;

import lombok.*;
import java.math.BigDecimal;

/** DTO matching account-service's BalanceOperationRequest */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BalanceOperationRequest {
    private BigDecimal amount;
    private String transactionRef;
}
