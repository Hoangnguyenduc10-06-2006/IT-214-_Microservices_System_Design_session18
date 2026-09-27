package com.rikkeibank.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_account", nullable = false)
    private String recipientAccount;

    @Column(nullable = false)
    private String type; // TRANSFER_RECEIVED, TRANSFER_SENT, TRANSFER_FAILED

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "transaction_ref")
    private String transactionRef;

    @Builder.Default
    @Column(name = "read_status")
    private boolean readStatus = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist protected void onCreate() { createdAt = LocalDateTime.now(); }
}
