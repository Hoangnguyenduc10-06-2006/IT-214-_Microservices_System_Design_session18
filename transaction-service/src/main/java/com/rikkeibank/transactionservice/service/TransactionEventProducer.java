package com.rikkeibank.transactionservice.service;

import com.rikkeibank.transactionservice.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka producer for publishing transaction events.
 * Events are consumed by notification-service (Event-driven Architecture).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionEventProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    private static final String TOPIC = "transaction-events";

    public void publishTransactionEvent(TransactionEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.getTransactionRef(), event);
            log.info("Published event to Kafka topic '{}': type={}, ref={}",
                    TOPIC, event.getEventType(), event.getTransactionRef());
        } catch (Exception e) {
            log.error("Failed to publish Kafka event: {}", e.getMessage());
            // Don't throw - Kafka failure shouldn't fail the transaction
        }
    }
}
