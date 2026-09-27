package com.rikkeibank.notificationservice.consumer;

import com.rikkeibank.notificationservice.dto.TransactionEvent;
import com.rikkeibank.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer for transaction events.
 * Implements Event-driven Architecture: reacts to events from transaction-service
 * and creates notifications for the involved accounts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "transaction-events", groupId = "notification-group")
    public void handleTransactionEvent(TransactionEvent event) {
        log.info("Received Kafka event: type={}, ref={}, source={}, dest={}",
                event.getEventType(), event.getTransactionRef(),
                event.getSourceAccountNumber(), event.getDestinationAccountNumber());

        switch (event.getEventType()) {
            case "TRANSFER_COMPLETED" -> handleTransferCompleted(event);
            case "TRANSFER_FAILED" -> handleTransferFailed(event);
            default -> log.warn("Unknown event type: {}", event.getEventType());
        }
    }

    private void handleTransferCompleted(TransactionEvent event) {
        // Notification for SENDER (debit)
        String senderMsg = String.format(
                "Biến động số dư: -%s %s chuyển khoản đến TK %s. Ref: %s",
                event.getAmount(), event.getCurrency(),
                event.getDestinationAccountNumber(), event.getTransactionRef());
        notificationService.createNotification(
                event.getSourceAccountNumber(), "TRANSFER_SENT", senderMsg, event.getTransactionRef());

        // Notification for RECEIVER (credit)
        String receiverMsg = String.format(
                "Biến động số dư: +%s %s nhận từ TK %s. Ref: %s",
                event.getAmount(), event.getCurrency(),
                event.getSourceAccountNumber(), event.getTransactionRef());
        notificationService.createNotification(
                event.getDestinationAccountNumber(), "TRANSFER_RECEIVED", receiverMsg, event.getTransactionRef());

        log.info("Created notifications for COMPLETED transfer ref={}", event.getTransactionRef());
    }

    private void handleTransferFailed(TransactionEvent event) {
        String failMsg = String.format(
                "Giao dịch thất bại: Chuyển %s %s đến TK %s không thành công. Số dư đã hoàn trả. Ref: %s",
                event.getAmount(), event.getCurrency(),
                event.getDestinationAccountNumber(), event.getTransactionRef());
        notificationService.createNotification(
                event.getSourceAccountNumber(), "TRANSFER_FAILED", failMsg, event.getTransactionRef());

        log.info("Created notification for FAILED transfer ref={}", event.getTransactionRef());
    }
}
