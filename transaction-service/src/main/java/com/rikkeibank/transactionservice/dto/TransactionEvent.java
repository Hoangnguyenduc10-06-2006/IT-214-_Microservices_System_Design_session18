package com.rikkeibank.transactionservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Event published to Kafka after transaction completion/failure.
 * Consumed by notification-service for event-driven notifications.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionEvent {
    private String eventType; // TRANSFER_COMPLETED, TRANSFER_FAILED
    private String transactionRef;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String description;
    private String timestamp;
}
