package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.config.properties.NotificationProperties;
import com.mazurek.eventOrganizer.exception.notification.NotificationDeliveryNotFoundException;
import com.mazurek.eventOrganizer.exception.notification.NotificationDeliveryNotProcessableException;
import com.mazurek.eventOrganizer.exception.notification.NotificationSendFailException;
import com.mazurek.eventOrganizer.notification.delivery.NotificationDeliveryClaim;
import com.mazurek.eventOrganizer.notification.delivery.NotificationSendResult;
import com.mazurek.eventOrganizer.notification.delivery.NotificationSenderDispatcher;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeliveryClaimRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeliveryRepository;
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
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationDeliveryServiceImpl implements NotificationDeliveryService {

    private static final int MAX_ERROR_LENGTH = 3000;

    private final Clock clock;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationDeliveryClaimRepository notificationDeliveryClaimRepository;
    private final NotificationPreferenceService notificationPreferenceService;
    private final NotificationSenderDispatcher notificationSenderDispatcher;
    private final NotificationChannelAvailability notificationChannelAvailability;
    private final NotificationRetryPolicy notificationRetryPolicy;
    private final NotificationProperties notificationProperties;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void createDeliveries(Notification notification) {
        Set<NotificationChannel> channels = notificationPreferenceService.getEnabledExternalChannels(
                notification.getRecipientId(),
                notification.getResourceType()
        );

        Instant createdAt = clock.instant();
        List<NotificationDelivery> deliveries = channels.stream()
                .map(channel -> NotificationDelivery.builder()
                        .channel(channel)
                        .status(NotificationDeliveryStatus.PENDING)
                        .attemptCount(0)
                        .createdAt(createdAt)
                        .build())
                .toList();

        deliveries.forEach(notification::addDelivery);
        notificationDeliveryRepository.saveAll(deliveries);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processPendingDeliveries() {
        NotificationProperties.Delivery settings = notificationProperties.getDelivery();
        Instant now = clock.instant();
        Instant abandonedBefore = now.minus(settings.getProcessingTimeout());

        List<NotificationDeliveryClaim> claims = notificationDeliveryClaimRepository.claimBatch(
                now,
                abandonedBefore,
                settings.getBatchSize(),
                settings.getMaxAttempts()
        );

        for (NotificationDeliveryClaim claim : claims) {
            try {
                processClaim(claim);
            } catch (RuntimeException exception) {
                log.error(
                        "Could not process claimed notification delivery {}. It will be recoverable after the processing timeout.",
                        claim.deliveryId(),
                        exception
                );
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processDelivery(UUID deliveryId) {
        if (!notificationDeliveryRepository.existsById(deliveryId)) {
            throw new NotificationDeliveryNotFoundException();
        }

        NotificationProperties.Delivery settings = notificationProperties.getDelivery();
        Instant now = clock.instant();
        NotificationDeliveryClaim claim = notificationDeliveryClaimRepository.claimOne(
                        deliveryId,
                        now,
                        now.minus(settings.getProcessingTimeout()),
                        settings.getMaxAttempts()
                )
                .orElseThrow(NotificationDeliveryNotProcessableException::new);

        processClaim(claim);
    }

    private void processClaim(NotificationDeliveryClaim claim) {
        Optional<DispatchRequest> request = transactionTemplate.execute(status ->
                notificationDeliveryRepository.findClaimedForDispatch(
                                claim.deliveryId(),
                                claim.claimToken()
                        )
                        .map(delivery -> new DispatchRequest(
                                delivery.getChannel(),
                                delivery.getNotification()
                        ))
        );

        if (request == null || request.isEmpty()) {
            throw new NotificationDeliveryNotProcessableException();
        }

        NotificationSendResult result = send(request.get());
        completeClaim(claim, result);
    }

    private NotificationSendResult send(DispatchRequest request) {
        if (!notificationChannelAvailability.isAvailable(request.channel())) {
            return NotificationSendResult.skipped(
                    "Notification channel %s is disabled.".formatted(request.channel())
            );
        }

        try {
            return notificationSenderDispatcher.send(request.channel(), request.notification());
        } catch (NotificationSendFailException exception) {
            return NotificationSendResult.retryableFailure(exception.getMessage());
        }
    }

    private void completeClaim(
            NotificationDeliveryClaim claim,
            NotificationSendResult result
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            Optional<NotificationDelivery> claimedDelivery = notificationDeliveryRepository
                    .findClaimedForDispatch(claim.deliveryId(), claim.claimToken());

            if (claimedDelivery.isEmpty()) {
                log.warn(
                        "Ignoring stale completion for notification delivery {}.",
                        claim.deliveryId()
                );
                return;
            }

            NotificationDelivery delivery = claimedDelivery.get();
            switch (result.outcome()) {
                case SENT -> delivery.markSent(result.providerMessageId(), clock.instant());
                case SKIPPED -> delivery.markSkipped(normalizeError(
                        result.errorMessage(),
                        "Notification delivery was skipped."
                ));
                case RETRYABLE_FAILURE -> markRetryableFailure(
                        delivery,
                        normalizeError(result.errorMessage(), "Retryable notification delivery failure.")
                );
                case PERMANENT_FAILURE -> delivery.markDead(normalizeError(
                        result.errorMessage(),
                        "Permanent notification delivery failure."
                ));
            }
        });
    }

    private void markRetryableFailure(NotificationDelivery delivery, String errorMessage) {
        if (notificationRetryPolicy.attemptsExhausted(delivery.getAttemptCount())) {
            delivery.markDead(errorMessage);
            return;
        }

        Duration retryDelay = notificationRetryPolicy.delayAfterFailedAttempt(
                delivery.getAttemptCount()
        );
        delivery.markFailed(errorMessage, clock.instant().plus(retryDelay));
    }

    private String normalizeError(String errorMessage, String fallback) {
        String normalized = errorMessage == null || errorMessage.isBlank()
                ? fallback
                : errorMessage;

        return normalized.length() <= MAX_ERROR_LENGTH
                ? normalized
                : normalized.substring(0, MAX_ERROR_LENGTH);
    }

    private record DispatchRequest(
            NotificationChannel channel,
            Notification notification
    ) {
    }
}
