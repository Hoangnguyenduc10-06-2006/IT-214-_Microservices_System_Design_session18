package com.rikkeibank.notificationservice.service;

import com.rikkeibank.notificationservice.entity.Notification;
import com.rikkeibank.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = Notification.builder()
                .id(1L)
                .recipientAccount("RKB1000000001")
                .type("TRANSFER_RECEIVED")
                .message("Biến động số dư: +1,000,000 VND")
                .transactionRef("TX-999")
                .readStatus(false)
                .build();
    }

    @Test
    @DisplayName("Create Notification: Should save notification")
    void testCreateNotification() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

        Notification created = notificationService.createNotification(
                "RKB1000000001", "TRANSFER_RECEIVED", "Biến động số dư: +1,000,000 VND", "TX-999");

        assertNotNull(created);
        assertEquals("RKB1000000001", created.getRecipientAccount());
        assertFalse(created.isReadStatus());
    }

    @Test
    @DisplayName("Mark As Read: Should update readStatus to true")
    void testMarkAsRead() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification read = notificationService.markAsRead(1L);

        assertNotNull(read);
        assertTrue(read.isReadStatus());
    }

    @Test
    @DisplayName("Get Notifications By Account: Should return list")
    void testGetNotificationsByAccount() {
        when(notificationRepository.findByRecipientAccountOrderByCreatedAtDesc("RKB1000000001"))
                .thenReturn(List.of(sampleNotification));

        List<Notification> list = notificationService.getNotificationsByAccount("RKB1000000001");

        assertEquals(1, list.size());
        assertEquals("TRANSFER_RECEIVED", list.get(0).getType());
    }
}
