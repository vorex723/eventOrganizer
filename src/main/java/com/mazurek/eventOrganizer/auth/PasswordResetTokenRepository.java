package com.mazurek.eventOrganizer.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetToken token where token.user.id = :userId")
    Optional<PasswordResetToken> findByUserIdForUpdate(@Param("userId") UUID userId);

    @Query("select token.user.id from PasswordResetToken token where token.tokenHash = :tokenHash")
    Optional<UUID> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    boolean existsByTokenHashAndUserIdAndExpirationDateAfter(String tokenHash, UUID userId, Instant now);

    default boolean isCurrentToken(UUID rawToken, UUID userId, Instant now) {
        return existsByTokenHashAndUserIdAndExpirationDateAfter(AuthTokenHash.sha256(rawToken), userId, now);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    default Optional<PasswordResetToken> findByToken(UUID token) {
        return findByTokenHash(AuthTokenHash.sha256(token));
    }

    @Modifying
    @Query("delete from PasswordResetToken token where token.expirationDate < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
