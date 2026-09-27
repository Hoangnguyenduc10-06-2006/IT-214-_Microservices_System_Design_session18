package com.rikkeibank.notificationservice.consumer;

import com.rikkeibank.notificationservice.dto.TransactionEvent;
import com.rikkeibank.notificationservice.entity.Notification;
import com.rikkeibank.notificationservice.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TransactionEventConsumer eventConsumer;

    @Test
    @DisplayName("Kafka Consumer: TRANSFER_COMPLETED should create notifications for both sender and receiver")
    void testHandleTransferCompleted() {
        TransactionEvent event = TransactionEvent.builder()
                .eventType("TRANSFER_COMPLETED")
                .transactionRef("TX-SUCCESS-001")
                .sourceAccountNumber("RKB001")
                .destinationAccountNumber("RKB002")
                .amount(new BigDecimal("500000.00"))
                .currency("VND")
                .status("COMPLETED")
                .build();

        when(notificationService.createNotification(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mock(Notification.class));

        eventConsumer.handleTransactionEvent(event);

        // Verify sender notified with TRANSFER_SENT
        verify(notificationService, times(1)).createNotification(
                eq("RKB001"), eq("TRANSFER_SENT"), anyString(), eq("TX-SUCCESS-001"));

        // Verify receiver notified with TRANSFER_RECEIVED
        verify(notificationService, times(1)).createNotification(
                eq("RKB002"), eq("TRANSFER_RECEIVED"), anyString(), eq("TX-SUCCESS-001"));
    }

    @Test
    @DisplayName("Kafka Consumer: TRANSFER_FAILED should create failure notification for sender")
    void testHandleTransferFailed() {
        TransactionEvent event = TransactionEvent.builder()
                .eventType("TRANSFER_FAILED")
                .transactionRef("TX-FAIL-001")
                .sourceAccountNumber("RKB001")
                .destinationAccountNumber("RKB002")
                .amount(new BigDecimal("500000.00"))
                .currency("VND")
                .status("FAILED")
                .build();

        when(notificationService.createNotification(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mock(Notification.class));

        eventConsumer.handleTransactionEvent(event);

        verify(notificationService, times(1)).createNotification(
                eq("RKB001"), eq("TRANSFER_FAILED"), anyString(), eq("TX-FAIL-001"));
    }
}
