package com.mazurek.eventOrganizer.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from EmailChangeToken token where token.user.id = :userId")
    Optional<EmailChangeToken> findByUserIdForUpdate(@Param("userId") UUID userId);

    @Query("select token.user.id from EmailChangeToken token where token.tokenHash = :tokenHash")
    Optional<UUID> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    boolean existsByTokenHashAndUserIdAndPendingEmailIgnoreCaseAndExpirationDateAfter(
            String tokenHash, UUID userId, String pendingEmail, Instant now);

    default boolean isCurrentToken(UUID rawToken, UUID userId, String pendingEmail, Instant now) {
        return existsByTokenHashAndUserIdAndPendingEmailIgnoreCaseAndExpirationDateAfter(
                AuthTokenHash.sha256(rawToken), userId, pendingEmail, now);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailChangeToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from EmailChangeToken token where token.expirationDate < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
