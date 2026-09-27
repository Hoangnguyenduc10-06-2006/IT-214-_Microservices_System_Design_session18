package com.rikkeibank.notificationservice.service;

import com.rikkeibank.notificationservice.entity.Notification;
import com.rikkeibank.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification createNotification(String recipientAccount, String type,
                                            String message, String transactionRef) {
        Notification notification = Notification.builder()
                .recipientAccount(recipientAccount)
                .type(type)
                .message(message)
                .transactionRef(transactionRef)
                .readStatus(false)
                .build();
        return notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsByAccount(String accountNumber) {
        return notificationRepository.findByRecipientAccountOrderByCreatedAtDesc(accountNumber);
    }

    public Notification markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + id));
        notification.setReadStatus(true);
        return notificationRepository.save(notification);
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }
}
