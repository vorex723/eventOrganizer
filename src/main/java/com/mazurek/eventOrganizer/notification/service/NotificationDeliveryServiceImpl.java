package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeliveryRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationDeliveryServiceImpl implements NotificationDeliveryService {
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationDeviceRepository  notificationDeviceRepository;


    @Override
    public void createDeliveries(Notification notification) {


    }

    @Override
    public void processPendingDeliveries() {

    }

    @Override
    public void processDelivery(UUID deliveryId) {

    }
}

