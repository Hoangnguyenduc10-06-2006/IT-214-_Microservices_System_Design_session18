package com.rikkeibank.notificationservice.controller;

import com.rikkeibank.notificationservice.entity.Notification;
import com.rikkeibank.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** Get notifications by account number */
    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<Notification>> getNotificationsByAccount(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(notificationService.getNotificationsByAccount(accountNumber));
    }

    /** Get all notifications (for testing) */
    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    /** Mark notification as read */
    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }
}
