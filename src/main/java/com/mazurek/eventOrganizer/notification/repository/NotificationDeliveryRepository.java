package com.mazurek.eventOrganizer.notification.repository;

import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {
    @Query("""
            select nd.id from NotificationDelivery nd
            where nd.status = com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.PENDING
               or (
                   nd.status = com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.FAILED
                   and nd.nextAttemptAt <= :now
               )
            order by nd.createdAt asc
            """)
    Page<UUID> findProcessableDeliveryIds(
            @Param("now") Instant now,
            Pageable pageable
    );

}
