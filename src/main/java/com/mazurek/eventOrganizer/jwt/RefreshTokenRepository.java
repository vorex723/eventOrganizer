package com.mazurek.eventOrganizer.jwt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** @deprecated Raw refresh credentials are intentionally not persisted. */
    @Deprecated
    default Optional<RefreshToken> findByToken(String token) {
        return findByTokenHash(RefreshTokenHash.sha256(token)).map(refreshToken -> {
            refreshToken.setRawToken(token);
            return refreshToken;
        });
    }

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findWithLockByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId")
    void revokeAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.deviceType = :deviceType")
    void revokeAllByUserIdAndDeviceType(@Param("userId") UUID userId, @Param("deviceType") DeviceType deviceType);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.deviceType IN :deviceTypes")
    void revokeAllByUserIdAndDeviceTypeIn(@Param("userId") UUID userId, @Param("deviceTypes") List<DeviceType> deviceTypes);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.familyId = :familyId")
    void revokeAllByFamilyId(@Param("familyId") UUID familyId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    void deleteExpiredTokens(@Param("now") Instant now);
}
