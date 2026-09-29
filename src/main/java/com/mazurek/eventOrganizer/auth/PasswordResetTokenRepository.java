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
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    default Optional<PasswordResetToken> findByToken(UUID token) {
        return findByTokenHash(AuthTokenHash.sha256(token));
    }

    @Modifying
    @Query("delete from PasswordResetToken token where token.expirationDate < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
