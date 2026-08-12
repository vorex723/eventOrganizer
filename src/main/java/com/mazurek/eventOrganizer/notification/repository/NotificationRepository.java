package com.mazurek.eventOrganizer.notification.repository;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findById(UUID id);
    Page<Notification> findByRecipientId(UUID userId, Pageable pageable);
}

