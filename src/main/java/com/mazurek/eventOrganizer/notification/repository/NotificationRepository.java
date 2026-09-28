package com.mazurek.eventOrganizer.notification.repository;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findById(UUID id);
    Page<Notification> findByRecipientId(UUID userId, Pageable pageable);
    long countByRecipientIdAndReadAtIsNull(UUID recipientId);
    Optional<Notification> findByIdAndRecipientId(UUID notificationId, UUID recipientId);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            update Notification n
            set n.readAt = :readAt
            where n.readAt is null
            and n.recipientId = :recipientId
            """
    )
    int markAllAsRead(@Param("recipientId")UUID recipientId, @Param("readAt") Instant readAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            delete from notifications notification
            where notification.created_at < :completedBefore
              and not exists (
                  select 1
                  from notification_deliveries delivery
                  where delivery.notification_id = notification.id
                    and (
                        delivery.status in ('PENDING', 'FAILED', 'PROCESSING')
                        or (delivery.status = 'DEAD' and delivery.created_at >= :deadBefore)
                    )
              )
            """, nativeQuery = true)
    int deleteCompletedBefore(
            @Param("completedBefore") Instant completedBefore,
            @Param("deadBefore") Instant deadBefore
    );

}
