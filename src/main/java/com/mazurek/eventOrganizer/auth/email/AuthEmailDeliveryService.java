package com.mazurek.eventOrganizer.auth.email;

import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.auth.PasswordResetTokenRepository;
import com.mazurek.eventOrganizer.auth.EmailChangeTokenRepository;
import com.mazurek.eventOrganizer.utils.EncryptionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthEmailDeliveryService {

    private static final int MAX_ERROR_LENGTH = 3000;

    private final AuthEmailDeliveryRepository authEmailDeliveryRepository;
    private final AuthEmailDeliveryClaimRepository authEmailDeliveryClaimRepository;
    private final AuthEmailSender authEmailSender;
    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailChangeTokenRepository emailChangeTokenRepository;
    private final EncryptionUtils encryptionUtils;
    private final AuthProperties authProperties;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public void enqueue(
            UUID userId,
            String recipientEmail,
            AuthEmailType type,
            UUID rawToken
    ) {
        Instant now = clock.instant();
        authEmailDeliveryRepository.save(AuthEmailDelivery.builder()
                .userId(userId)
                .recipientEmail(recipientEmail)
                .type(type)
                .encryptedToken(encryptionUtils.encryptMessage(rawToken.toString()))
                .status(AuthEmailDeliveryStatus.PENDING)
                .attemptCount(0)
                .createdAt(now)
                .build());
    }

    @Transactional
    public void cancelPending(UUID userId, AuthEmailType type) {
        authEmailDeliveryRepository.cancelProcessableByUserIdAndType(userId, type);
    }

    @Transactional(readOnly = true)
    public boolean wasRecentlyRequested(UUID userId, AuthEmailType type) {
        return authEmailDeliveryRepository.existsByUserIdAndTypeAndCreatedAtAfter(
                userId,
                type,
                clock.instant().minus(authProperties.getEmail().getResendCooldown())
        );
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processPendingDeliveries() {
        AuthProperties.Email settings = authProperties.getEmail();
        Instant now = clock.instant();
        List<AuthEmailDeliveryClaim> claims = authEmailDeliveryClaimRepository.claimBatch(
                now,
                now.minus(settings.getProcessingTimeout()),
                settings.getBatchSize(),
                settings.getMaxAttempts()
        );

        for (AuthEmailDeliveryClaim claim : claims) {
            try {
                processClaim(claim);
            } catch (RuntimeException exception) {
                log.error(
                        "Could not process claimed auth email delivery {}. It will be recoverable after the processing timeout.",
                        claim.deliveryId(),
                        exception
                );
            }
        }
    }

    private void processClaim(AuthEmailDeliveryClaim claim) {
        Optional<DispatchRequest> request = transactionTemplate.execute(status ->
                authEmailDeliveryRepository.findClaimedForDispatch(claim.deliveryId(), claim.claimToken())
                        .flatMap(delivery -> toDispatchRequestOrCancel(delivery))
        );

        if (request == null || request.isEmpty()) {
            return;
        }

        DispatchRequest dispatchRequest = request.get();
        AuthEmailSendResult result = authEmailSender.send(
                dispatchRequest.type(),
                dispatchRequest.recipientEmail(),
                dispatchRequest.rawToken()
        );
        completeClaim(claim, result);
    }

    private void completeClaim(AuthEmailDeliveryClaim claim, AuthEmailSendResult result) {
        transactionTemplate.executeWithoutResult(status -> authEmailDeliveryRepository
                .findClaimedForDispatch(claim.deliveryId(), claim.claimToken())
                .ifPresentOrElse(
                        delivery -> applyResult(delivery, result),
                        () -> log.warn("Ignoring stale completion for auth email delivery {}.", claim.deliveryId())
                ));
    }

    private Optional<DispatchRequest> toDispatchRequestOrCancel(AuthEmailDelivery delivery) {
        String rawToken;
        UUID token;
        try {
            rawToken = encryptionUtils.decryptMessage(delivery.getEncryptedToken());
            token = UUID.fromString(rawToken);
        } catch (RuntimeException exception) {
            log.error("Auth email delivery {} has an unreadable token payload.", delivery.getId(), exception);
            delivery.markDead("Auth email token payload is unreadable.");
            return Optional.empty();
        }

        boolean isCurrentAndValid = switch (delivery.getType()) {
            case ACCOUNT_ACTIVATION -> activationTokenRepository.findByToken(token)
                    .filter(current -> current.getUser().getId().equals(delivery.getUserId()))
                    .filter(current -> !current.isExpired(clock.instant()))
                    .isPresent();
            case PASSWORD_RESET -> passwordResetTokenRepository.findByToken(token)
                    .filter(current -> current.getUser().getId().equals(delivery.getUserId()))
                    .filter(current -> !current.isExpired(clock.instant()))
                    .isPresent();
            case EMAIL_CHANGE_CONFIRMATION -> emailChangeTokenRepository.findByToken(token)
                    .filter(current -> current.getUser().getId().equals(delivery.getUserId()))
                    .filter(current -> current.getPendingEmail().equalsIgnoreCase(delivery.getRecipientEmail()))
                    .filter(current -> !current.isExpired(clock.instant()))
                    .isPresent();
        };

        if (!isCurrentAndValid) {
            delivery.cancel();
            return Optional.empty();
        }

        return Optional.of(new DispatchRequest(
                delivery.getType(),
                delivery.getRecipientEmail(),
                rawToken
        ));
    }

    private void applyResult(AuthEmailDelivery delivery, AuthEmailSendResult result) {
        switch (result.outcome()) {
            case SENT -> delivery.markSent(result.providerMessageId(), clock.instant());
            case PERMANENT_FAILURE -> delivery.markDead(normalizeError(
                    result.errorMessage(),
                    "Permanent auth email delivery failure."
            ));
            case RETRYABLE_FAILURE -> markRetryableFailure(delivery, normalizeError(
                    result.errorMessage(),
                    "Retryable auth email delivery failure."
            ));
        }
    }

    private void markRetryableFailure(AuthEmailDelivery delivery, String error) {
        AuthProperties.Email settings = authProperties.getEmail();
        if (delivery.getAttemptCount() >= settings.getMaxAttempts()) {
            delivery.markDead(error);
            return;
        }

        Duration delay = settings.getRetryDelays().get(delivery.getAttemptCount() - 1);
        delivery.markFailed(error, clock.instant().plus(delay));
    }

    private String normalizeError(String error, String fallback) {
        String normalized = error == null || error.isBlank() ? fallback : error;
        return normalized.length() <= MAX_ERROR_LENGTH
                ? normalized
                : normalized.substring(0, MAX_ERROR_LENGTH);
    }

    private record DispatchRequest(
            AuthEmailType type,
            String recipientEmail,
            String rawToken
    ) {
    }
}
