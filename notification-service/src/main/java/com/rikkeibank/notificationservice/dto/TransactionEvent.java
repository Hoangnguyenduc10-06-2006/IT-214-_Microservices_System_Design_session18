package com.rikkeibank.notificationservice.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * Event received from Kafka (published by transaction-service).
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionEvent {
    private String eventType;
    private String transactionRef;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String description;
    private String timestamp;
}
