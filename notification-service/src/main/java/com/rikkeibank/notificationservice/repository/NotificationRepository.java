package com.rikkeibank.notificationservice.repository;

import com.rikkeibank.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientAccountOrderByCreatedAtDesc(String recipientAccount);
}
