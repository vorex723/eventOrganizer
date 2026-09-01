package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.exception.notification.NotificationDeliveryNotFoundException;
import com.mazurek.eventOrganizer.exception.notification.NotificationDeliveryNotProcessableException;
import com.mazurek.eventOrganizer.exception.notification.NotificationSendFailException;
import com.mazurek.eventOrganizer.notification.delivery.NotificationSendRequest;
import com.mazurek.eventOrganizer.notification.delivery.NotificationSendResult;
import com.mazurek.eventOrganizer.notification.delivery.NotificationSenderDispatcher;
import com.mazurek.eventOrganizer.notification.domain.*;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationDeliveryServiceImpl implements NotificationDeliveryService {

    private final Clock clock;

    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationPreferenceService notificationPreferenceService;
    private final NotificationSenderDispatcher notificationSenderDispatcher;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void createDeliveries(Notification notification) {

        Set<NotificationChannel> channels = notificationPreferenceService.getEnabledExternalChannels(
                notification.getRecipientId(),
                notification.getResourceType());

        Instant createdAt = clock.instant();

        List<NotificationDelivery> deliveries = channels.stream().map(channel -> {
                    return NotificationDelivery.builder()
                            .channel(channel)
                            .status(NotificationDeliveryStatus.PENDING)
                            .attemptCount(0)
                            .createdAt(createdAt)
                            .build();
                })
                .toList();

        deliveries.forEach(notification::addDelivery);
        notificationDeliveryRepository.saveAll(deliveries);

    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processPendingDeliveries() {
        Instant now = clock.instant();
        List<UUID> deliveriesIds = notificationDeliveryRepository
                .findProcessableDeliveryIds(now, PageRequest.of(0, 100))
                .getContent();


        for (UUID deliveryId : deliveriesIds) {
            try{
                transactionTemplate.executeWithoutResult(status -> {
                    processDelivery(deliveryId);
                });
            }catch (NotificationDeliveryNotFoundException exception){
                log.debug("Notification delivery id not found {}", deliveryId);
            }
            catch (NotificationDeliveryNotProcessableException exception){
                log.debug("Notification delivery not processable {}", deliveryId);
            }
            catch (RuntimeException exception){
                log.error("Could not process delivery {}", deliveryId, exception);
            }
        }

    }

    @Override
    @Transactional
    public void processDelivery(UUID deliveryId) {

        NotificationDelivery notificationDelivery = notificationDeliveryRepository.findById(deliveryId)
                .orElseThrow(NotificationDeliveryNotFoundException::new);

        if (!List.of(NotificationDeliveryStatus.PENDING, NotificationDeliveryStatus.FAILED).contains(notificationDelivery.getStatus())) {
            throw new NotificationDeliveryNotProcessableException();
        }

        notificationDelivery.markProcessing();

        Notification notification = notificationDelivery.getNotification();
        NotificationSendRequest sendRequest = new NotificationSendRequest(
                notification.getId(),
                notification.getRecipientId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getResourceType(),
                notification.getResourceId(),
                Map.of()
        );

        NotificationSendResult notificationSendResult;
        try {
            notificationSendResult = notificationSenderDispatcher
                    .send(notificationDelivery.getChannel(), sendRequest);
        } catch (NotificationSendFailException exception) {
            markFailed(notificationDelivery, exception.getMessage());
            return;
        }

        if (notificationSendResult.success()) {
            Instant sentAt = clock.instant();
            notificationDelivery.markSent(notificationSendResult.providerMessageId(), sentAt);
        } else {
            markFailed(notificationDelivery, notificationSendResult.errorMessage());
        }
    }

    private void markFailed(NotificationDelivery notificationDelivery, String errorMessage) {
        int retryDelayHours = notificationDelivery.getAttemptCount() >= 5 ? 3 : 1;

        Instant nextAttempt = clock.instant()
                .plus(retryDelayHours, ChronoUnit.HOURS);

        notificationDelivery.markFailed(errorMessage, nextAttempt);
    }
}

