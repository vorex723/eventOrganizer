package com.mazurek.eventOrganizer.auth.email;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthEmailDeliveryRepository extends JpaRepository<AuthEmailDelivery, UUID> {

    @Query("""
            select delivery
            from AuthEmailDelivery delivery
            where delivery.id = :deliveryId
              and delivery.claimToken = :claimToken
              and delivery.status = com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.PROCESSING
            """)
    Optional<AuthEmailDelivery> findClaimedForDispatch(
            @Param("deliveryId") UUID deliveryId,
            @Param("claimToken") UUID claimToken
    );

    boolean existsByUserIdAndTypeAndCreatedAtAfter(
            UUID userId,
            AuthEmailType type,
            Instant createdAfter
    );

    @Modifying
    @Query("""
            update AuthEmailDelivery delivery
            set delivery.status = com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.CANCELLED,
                delivery.nextAttemptAt = null,
                delivery.processingStartedAt = null,
                delivery.claimToken = null
            where delivery.userId = :userId
              and delivery.type = :type
              and delivery.status in (
                    com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.PENDING,
                    com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.FAILED,
                    com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.PROCESSING
              )
            """)
    void cancelProcessableByUserIdAndType(
            @Param("userId") UUID userId,
            @Param("type") AuthEmailType type
    );

    @Modifying
    @Query("""
            delete from AuthEmailDelivery delivery
            where delivery.createdAt < :before
              and delivery.status in (
                    com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.SENT,
                    com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.DEAD,
                    com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.CANCELLED
              )
            """)
    int deleteCompletedBefore(@Param("before") Instant before);
}
