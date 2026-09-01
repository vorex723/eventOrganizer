package com.mazurek.eventOrganizer.notification.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationDeliveryStatus status;

    @Column(nullable = false)
    private int attemptCount;

    private Instant nextAttemptAt;
    private Instant sentAt;

    private String providerMessageId;

    @Column(length = 3000)
    private String lastError;

    @Column(nullable = false)
    private Instant createdAt;

    public void markProcessing() {
        this.status = NotificationDeliveryStatus.PROCESSING;
        this.attemptCount++;
    }

    public void markSent(String providerMessageId, Instant sentAt) {
        this.status = NotificationDeliveryStatus.SENT;
        this.providerMessageId = providerMessageId;
        this.sentAt = sentAt;
        this.lastError = null;
        this.nextAttemptAt = null;
    }

    public void markFailed(String error, Instant nextAttemptAt) {
        this.status = NotificationDeliveryStatus.FAILED;
        this.lastError = error;
        this.nextAttemptAt = nextAttemptAt;
    }

    public void markDead(String error) {
        this.status = NotificationDeliveryStatus.DEAD;
        this.lastError = error;
        this.nextAttemptAt = null;
    }

    public void markSkipped(String reason) {
        this.status = NotificationDeliveryStatus.SKIPPED;
        this.lastError = reason;
        this.nextAttemptAt = null;
    }

    public void incrementAttemptCount() {
        this.attemptCount++;
    }
}