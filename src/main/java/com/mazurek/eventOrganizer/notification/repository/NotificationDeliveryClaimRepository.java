package com.mazurek.eventOrganizer.notification.repository;

import com.mazurek.eventOrganizer.notification.delivery.NotificationDeliveryClaim;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationDeliveryClaimRepository {

    private static final String EXPIRE_EXHAUSTED_DELIVERIES = """
            update notification_deliveries
            set status = 'DEAD',
                next_attempt_at = null,
                processing_started_at = null,
                claim_token = null,
                last_error = coalesce(last_error, 'Maximum delivery attempts reached.')
            where attempt_count >= ?
              and (
                    status = 'PENDING'
                    or (status = 'FAILED' and next_attempt_at <= ?)
                    or (status = 'PROCESSING' and (processing_started_at is null or processing_started_at <= ?))
              )
            """;

    private static final String CLAIM_BATCH = """
            with candidates as (
                select id
                from notification_deliveries
                where attempt_count < ?
                  and (
                        status = 'PENDING'
                        or (status = 'FAILED' and next_attempt_at <= ?)
                        or (status = 'PROCESSING' and (processing_started_at is null or processing_started_at <= ?))
                  )
                order by created_at asc
                for update skip locked
                limit ?
            )
            update notification_deliveries delivery
            set status = 'PROCESSING',
                attempt_count = delivery.attempt_count + 1,
                next_attempt_at = null,
                processing_started_at = ?,
                claim_token = ?
            from candidates
            where delivery.id = candidates.id
            returning delivery.id, delivery.claim_token
            """;

    private static final String CLAIM_ONE = """
            with candidate as (
                select id
                from notification_deliveries
                where id = ?
                  and attempt_count < ?
                  and (
                        status = 'PENDING'
                        or (status = 'FAILED' and next_attempt_at <= ?)
                        or (status = 'PROCESSING' and (processing_started_at is null or processing_started_at <= ?))
                  )
                for update skip locked
            )
            update notification_deliveries delivery
            set status = 'PROCESSING',
                attempt_count = delivery.attempt_count + 1,
                next_attempt_at = null,
                processing_started_at = ?,
                claim_token = ?
            from candidate
            where delivery.id = candidate.id
            returning delivery.id, delivery.claim_token
            """;

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public List<NotificationDeliveryClaim> claimBatch(
            Instant now,
            Instant abandonedBefore,
            int batchSize,
            int maxAttempts
    ) {
        expireExhausted(now, abandonedBefore, maxAttempts);

        UUID claimToken = UUID.randomUUID();
        return jdbcTemplate.query(
                CLAIM_BATCH,
                (resultSet, rowNumber) -> new NotificationDeliveryClaim(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("claim_token", UUID.class)
                ),
                maxAttempts,
                Timestamp.from(now),
                Timestamp.from(abandonedBefore),
                batchSize,
                Timestamp.from(now),
                claimToken
        );
    }

    @Transactional
    public Optional<NotificationDeliveryClaim> claimOne(
            UUID deliveryId,
            Instant now,
            Instant abandonedBefore,
            int maxAttempts
    ) {
        expireExhausted(now, abandonedBefore, maxAttempts);

        UUID claimToken = UUID.randomUUID();
        return jdbcTemplate.query(
                CLAIM_ONE,
                (resultSet, rowNumber) -> new NotificationDeliveryClaim(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("claim_token", UUID.class)
                ),
                deliveryId,
                maxAttempts,
                Timestamp.from(now),
                Timestamp.from(abandonedBefore),
                Timestamp.from(now),
                claimToken
        ).stream().findFirst();
    }

    private void expireExhausted(Instant now, Instant abandonedBefore, int maxAttempts) {
        jdbcTemplate.update(
                EXPIRE_EXHAUSTED_DELIVERIES,
                maxAttempts,
                Timestamp.from(now),
                Timestamp.from(abandonedBefore)
        );
    }
}
