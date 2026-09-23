package com.mazurek.eventOrganizer.notification.repository;

import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

    @Query("""
            select delivery
            from NotificationDelivery delivery
            join fetch delivery.notification
            where delivery.id = :deliveryId
              and delivery.claimToken = :claimToken
              and delivery.status = com.mazurek.eventOrganizer.notification.domain.NotificationDeliveryStatus.PROCESSING
            """)
    Optional<NotificationDelivery> findClaimedForDispatch(
            @Param("deliveryId") UUID deliveryId,
            @Param("claimToken") UUID claimToken
    );

    @Query(value = """
            select count(*)
            from notification_deliveries
            where status in ('PENDING', 'FAILED')
              and (next_attempt_at is null or next_attempt_at <= :now)
            """, nativeQuery = true)
    long countDueForProcessing(@Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            delete from notification_deliveries
            where status = 'DEAD'
              and created_at < :deadBefore
            """, nativeQuery = true)
    int deleteDeadBefore(@Param("deadBefore") Instant deadBefore);

}
