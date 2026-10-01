package com.mazurek.eventOrganizer.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {
    @Query("select token.user.id from ActivationToken token where token.tokenHash = :tokenHash")
    Optional<UUID> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from ActivationToken token where token.user.id = :userId")
    Optional<ActivationToken> findByUserIdForUpdate(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ActivationToken> findByTokenHash(String tokenHash);

    default Optional<ActivationToken> findByToken(UUID token) {
        return findByTokenHash(AuthTokenHash.sha256(token));
    }

    boolean existsByTokenHashAndUserIdAndExpirationDateAfter(String tokenHash, UUID userId, Instant now);

    default boolean isCurrentToken(UUID rawToken, UUID userId, Instant now) {
        return existsByTokenHashAndUserIdAndExpirationDateAfter(AuthTokenHash.sha256(rawToken), userId, now);
    }

    @Modifying
    @Query("delete from ActivationToken token where token.expirationDate < :before")
    int deleteExpiredBefore(@Param("before") Instant before);

}
