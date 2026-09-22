package com.mazurek.eventOrganizer.auth.email;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AuthEmailDeliveryClaimRepository {

    private static final String EXPIRE_EXHAUSTED = """
            update auth_email_deliveries
            set status = 'DEAD', next_attempt_at = null, processing_started_at = null, claim_token = null,
                last_error = coalesce(last_error, 'Maximum auth email attempts reached.')
            where attempt_count >= ?
              and (status = 'PENDING'
                   or (status = 'FAILED' and next_attempt_at <= ?)
                   or (status = 'PROCESSING' and (processing_started_at is null or processing_started_at <= ?)))
            """;

    private static final String CLAIM_BATCH = """
            with candidates as (
                select id
                from auth_email_deliveries
                where attempt_count < ?
                  and (status = 'PENDING'
                       or (status = 'FAILED' and next_attempt_at <= ?)
                       or (status = 'PROCESSING' and (processing_started_at is null or processing_started_at <= ?)))
                order by created_at asc
                for update skip locked
                limit ?
            )
            update auth_email_deliveries delivery
            set status = 'PROCESSING', attempt_count = delivery.attempt_count + 1,
                next_attempt_at = null, processing_started_at = ?, claim_token = ?
            from candidates
            where delivery.id = candidates.id
            returning delivery.id, delivery.claim_token
            """;

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public List<AuthEmailDeliveryClaim> claimBatch(
            Instant now,
            Instant abandonedBefore,
            int batchSize,
            int maxAttempts
    ) {
        jdbcTemplate.update(
                EXPIRE_EXHAUSTED,
                maxAttempts,
                Timestamp.from(now),
                Timestamp.from(abandonedBefore)
        );

        UUID claimToken = UUID.randomUUID();
        return jdbcTemplate.query(
                CLAIM_BATCH,
                (resultSet, rowNumber) -> new AuthEmailDeliveryClaim(
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
}
