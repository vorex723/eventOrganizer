package com.mazurek.eventOrganizer.auth.email;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_email_deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthEmailDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(nullable = false)
    private UUID userId;

    @Size(max = 255)
    @NotBlank
    @Column(nullable = false, length = 255)
    private String recipientEmail;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuthEmailType type;

    @Size(max = 4096)
    @NotBlank
    @Column(nullable = false, length = 4096)
    private String encryptedToken;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AuthEmailDeliveryStatus status;

    @PositiveOrZero
    @Column(nullable = false)
    private int attemptCount;

    private Instant nextAttemptAt;
    private Instant processingStartedAt;
    private UUID claimToken;
    private Instant sentAt;

    @Size(max = 3000)
    @Column(length = 3000, nullable = true)
    private String lastError;

    @NotNull
    @Column(nullable = false)
    private Instant createdAt;

    public void markSent(Instant now) {
        status = AuthEmailDeliveryStatus.SENT;
        sentAt = now;
        lastError = null;
        nextAttemptAt = null;
        clearClaim();
    }

    public void markFailed(String error, Instant nextAttempt) {
        status = AuthEmailDeliveryStatus.FAILED;
        lastError = error;
        nextAttemptAt = nextAttempt;
        clearClaim();
    }

    public void markDead(String error) {
        status = AuthEmailDeliveryStatus.DEAD;
        lastError = error;
        nextAttemptAt = null;
        clearClaim();
    }

    public void cancel() {
        status = AuthEmailDeliveryStatus.CANCELLED;
        nextAttemptAt = null;
        clearClaim();
    }

    private void clearClaim() {
        processingStartedAt = null;
        claimToken = null;
    }
}
