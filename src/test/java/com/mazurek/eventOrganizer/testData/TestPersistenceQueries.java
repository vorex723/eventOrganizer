package com.mazurek.eventOrganizer.testData;

import com.mazurek.eventOrganizer.auth.EmailChangeToken;
import com.mazurek.eventOrganizer.auth.PasswordResetToken;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import com.mazurek.eventOrganizer.jwt.RefreshToken;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Read-only persistence assertions that are not part of the application's repository API. */
@Component
@Transactional(readOnly = true)
public class TestPersistenceQueries {

    @PersistenceContext
    private EntityManager entityManager;

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
