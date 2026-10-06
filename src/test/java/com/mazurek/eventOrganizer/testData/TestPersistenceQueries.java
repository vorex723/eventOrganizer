package com.mazurek.eventOrganizer.testData;

import com.mazurek.eventOrganizer.auth.EmailChangeToken;
import com.mazurek.eventOrganizer.auth.ActivationToken;
import com.mazurek.eventOrganizer.auth.PasswordResetToken;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import com.mazurek.eventOrganizer.jwt.RefreshToken;
import com.mazurek.eventOrganizer.testData.builders.ActivationTokenTestBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;

/** Read-only persistence assertions that are not part of the application's repository API. */
@Component
@Transactional(readOnly = true)
public class TestPersistenceQueries {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Committed row values, not managed entities; sequence gaps are deliberately outside this contract. */
    public Map<String, List<Map<String, Object>>> conversationState() {
        return Map.of(
                "conversations", jdbcTemplate.queryForList("SELECT * FROM conversations ORDER BY id"),
                "participants", jdbcTemplate.queryForList("SELECT * FROM conversation_participant ORDER BY id"),
                "pairs", jdbcTemplate.queryForList("SELECT * FROM direct_conversation_pair ORDER BY id"),
                "messages", jdbcTemplate.queryForList("SELECT * FROM messages ORDER BY id"),
                "notifications", jdbcTemplate.queryForList("SELECT * FROM notifications ORDER BY id"),
                "deliveries", jdbcTemplate.queryForList("SELECT * FROM notification_deliveries ORDER BY id")
        );
    }

    /** Thread workflows: full row values include counters, versions, dates, relationships and outbox. */
    public Map<String, List<Map<String, Object>>> threadState() {
        return Map.of(
                "threads", jdbcTemplate.queryForList("SELECT * FROM threads ORDER BY id"),
                "replies", jdbcTemplate.queryForList("SELECT * FROM thread_replies ORDER BY id"),
                "events", jdbcTemplate.queryForList("SELECT * FROM events ORDER BY id"),
                "attendees", jdbcTemplate.queryForList("SELECT * FROM event_user ORDER BY event_id, user_id"),
                "notifications", jdbcTemplate.queryForList("SELECT * FROM notifications ORDER BY id"),
                "deliveries", jdbcTemplate.queryForList("SELECT * FROM notification_deliveries ORDER BY id")
        );
    }

    /** Hex encoding keeps bytea comparable by value across fresh JDBC reads, without losing bytes. */
    public Map<String, List<Map<String, Object>>> fileState() {
        return Map.of(
                "files", jdbcTemplate.queryForList("""
                        SELECT id, event_id, user_id, user_file_name, original_file_name, content_type,
                               encode(content, 'hex') AS content, upload_date_time
                        FROM files ORDER BY id
                        """),
                "events", jdbcTemplate.queryForList("SELECT * FROM events ORDER BY id"),
                "attendees", jdbcTemplate.queryForList("SELECT * FROM event_user ORDER BY event_id, user_id"),
                "notifications", jdbcTemplate.queryForList("SELECT * FROM notifications ORDER BY id"),
                "deliveries", jdbcTemplate.queryForList("SELECT * FROM notification_deliveries ORDER BY id")
        );
    }

    /** Tag reads must not change canonical labels, event data or the join table. */
    public Map<String, List<Map<String, Object>>> tagState() {
        return Map.of(
                "tags", jdbcTemplate.queryForList("SELECT * FROM tags ORDER BY id"),
                "events", jdbcTemplate.queryForList("SELECT * FROM events ORDER BY id"),
                "eventTags", jdbcTemplate.queryForList("SELECT * FROM event_tag ORDER BY event_id, tag_id")
        );
    }

    /** Complete comparable rows, including preference aggregate versions; no managed entity snapshot. */
    public Map<String, List<Map<String, Object>>> notificationState() {
        return Map.of(
                "notifications", jdbcTemplate.queryForList("SELECT * FROM notifications ORDER BY id"),
                "deliveries", jdbcTemplate.queryForList("SELECT * FROM notification_deliveries ORDER BY id"),
                "devices", jdbcTemplate.queryForList("SELECT * FROM notification_devices ORDER BY id"),
                "preferences", jdbcTemplate.queryForList("SELECT * FROM notification_preferences ORDER BY id"),
                "users", jdbcTemplate.queryForList("SELECT * FROM users ORDER BY id")
        );
    }

    public Optional<ActivationToken> findActivationToken(UUID rawToken) {
        ActivationToken probe = new ActivationTokenTestBuilder().id(null).user(null).unissued().build();
        probe.issue(rawToken, 0, Instant.EPOCH);
        return Optional.ofNullable(entityManager.createQuery(
                        "select token from ActivationToken token where token.tokenHash = :tokenHash",
                        ActivationToken.class)
                .setParameter("tokenHash", probe.getTokenHash())
                .getSingleResultOrNull());
    }

    public Optional<ActivationToken> findActivationTokenByUserEmail(String email) {
        return Optional.ofNullable(entityManager.createQuery(
                        "select token from ActivationToken token where lower(token.user.email) = lower(:email)",
                        ActivationToken.class)
                .setParameter("email", email)
                .getSingleResultOrNull());
    }

    public Optional<RefreshToken> findRefreshTokenByHash(String tokenHash) {
        return Optional.ofNullable(entityManager.createQuery(
                        "select token from RefreshToken token where token.tokenHash = :tokenHash",
                        RefreshToken.class)
                .setParameter("tokenHash", tokenHash)
                .getSingleResultOrNull());
    }

    public Optional<PasswordResetToken> findPasswordResetTokenByUserId(UUID userId) {
        return Optional.ofNullable(entityManager.createQuery(
                        "select token from PasswordResetToken token where token.user.id = :userId",
                        PasswordResetToken.class)
                .setParameter("userId", userId)
                .getSingleResultOrNull());
    }

    public Optional<EmailChangeToken> findEmailChangeTokenByUserId(UUID userId) {
        return Optional.ofNullable(entityManager.createQuery(
                        "select token from EmailChangeToken token where token.user.id = :userId",
                        EmailChangeToken.class)
                .setParameter("userId", userId)
                .getSingleResultOrNull());
    }

    public Optional<ConversationParticipant> findConversationParticipant(UUID conversationId, UUID userId) {
        return Optional.ofNullable(entityManager.createQuery("""
                        select participant from ConversationParticipant participant
                        where participant.conversation.id = :conversationId and participant.user.id = :userId
                        """, ConversationParticipant.class)
                .setParameter("conversationId", conversationId)
                .setParameter("userId", userId)
                .getSingleResultOrNull());
    }
}
