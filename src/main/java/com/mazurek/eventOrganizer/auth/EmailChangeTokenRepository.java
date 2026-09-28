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
    Optional<EmailChangeToken> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailChangeToken> findByTokenHash(String tokenHash);

    default Optional<EmailChangeToken> findByToken(UUID token) { return findByTokenHash(AuthTokenHash.sha256(token)); }

    @Modifying
    @Query("delete from EmailChangeToken token where token.expirationDate < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
