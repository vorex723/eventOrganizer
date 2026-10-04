package com.mazurek.eventOrganizer.notification.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notification_deliveries",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_notification_deliveries_target",
                columnNames = {"notification_id", "channel", "target_key"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Size(max = 320)
    @NotBlank
    @Column(name = "target_key", nullable = false, length = 320)
    private String targetKey;

    @Size(max = 320)
    @Column(name = "target_email", length = 320, nullable = true)
    private String targetEmail;

    private UUID targetDeviceId;

    @Size(max = 255)
    @Column(length = 255, nullable = true)
    private String targetInstallationId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationDeliveryStatus status;

    @PositiveOrZero
    @Column(nullable = false)
    private int attemptCount;

    private Instant nextAttemptAt;
    private Instant processingStartedAt;
    private UUID claimToken;
    private Instant sentAt;

    @Size(max = 255)
    @Column(nullable = true, length = 255)
    private String providerMessageId;

    @Size(max = 3000)
    @Column(length = 3000, nullable = true)
    private String lastError;

    @NotNull
    @Column(nullable = false)
    private Instant createdAt;

    public void markSent(String providerMessageId, Instant sentAt) {
        this.status = NotificationDeliveryStatus.SENT;
        this.providerMessageId = providerMessageId;
        this.sentAt = sentAt;
        this.lastError = null;
        this.nextAttemptAt = null;
        clearClaim();
    }

    public void markFailed(String error, Instant nextAttemptAt) {
        this.status = NotificationDeliveryStatus.FAILED;
        this.lastError = error;
        this.nextAttemptAt = nextAttemptAt;
        clearClaim();
    }

    public void markDead(String error) {
        this.status = NotificationDeliveryStatus.DEAD;
        this.lastError = error;
        this.nextAttemptAt = null;
        clearClaim();
    }

    public void markSkipped(String reason) {
        this.status = NotificationDeliveryStatus.SKIPPED;
        this.lastError = reason;
        this.nextAttemptAt = null;
        clearClaim();
    }

    private void clearClaim() {
        this.processingStartedAt = null;
        this.claimToken = null;
    }
}
