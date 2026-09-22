package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.config.properties.JwtProperties;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenExpiredException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenRevokedException;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.exception.user.UserBannedException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Transactional
    public IssuedRefreshToken issueRefreshToken(User user, DeviceType deviceType){
        return issueRefreshToken(user, deviceType, UUID.randomUUID());
    }

    /** @deprecated Use {@link #issueRefreshToken(User, DeviceType)} so callers only expose the raw value at issuance. */
    @Deprecated
    @Transactional
    public RefreshToken createRefreshToken(User user, DeviceType deviceType) {
        IssuedRefreshToken issuedRefreshToken = issueRefreshToken(user, deviceType);
        issuedRefreshToken.refreshToken().setRawToken(issuedRefreshToken.rawToken());
        return issuedRefreshToken.refreshToken();
    }

    private IssuedRefreshToken issueRefreshToken(User user, DeviceType deviceType, UUID familyId){

        Long expiration = deviceType.shouldRotateRefreshToken() ? jwtProperties.getRefreshShortExpiration() : jwtProperties.getRefreshLongExpiration();
        Instant tokenCreateDate = clock.instant();

        RefreshToken refreshToken = new RefreshToken();
        String rawToken = generateRawToken();
        refreshToken.setTokenHash(RefreshTokenHash.sha256(rawToken));
        refreshToken.setFamilyId(familyId);
        refreshToken.setUser(user);
        refreshToken.setDeviceType(deviceType);
        refreshToken.setCreatedAt(tokenCreateDate);
        refreshToken.setExpiryDate(tokenCreateDate.plusMillis(expiration));
        refreshToken.setLastUsedAt(tokenCreateDate);

        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);
        return new IssuedRefreshToken(savedRefreshToken == null ? refreshToken : savedRefreshToken, rawToken);
    }

    @Transactional
    public RefreshTokenUse useRefreshToken(String rawToken){
        RefreshToken refreshToken = refreshTokenRepository.findWithLockByTokenHash(RefreshTokenHash.sha256(rawToken))
                .orElseThrow(RefreshTokenNotFoundException::new);
        Instant now = clock.instant();

        if (refreshToken.isRevoked()) {
            if (refreshToken.getDeviceType().shouldRotateRefreshToken()) {
                refreshTokenRepository.revokeAllByFamilyId(refreshToken.getFamilyId());
            }
            throw new RefreshTokenRevokedException();
        }
        if (refreshToken.isExpired(now)) {
            throw new RefreshTokenExpiredException();
        }
        if (refreshToken.getUser().isBanned()) {
            throw new UserBannedException();
        }

        refreshToken.setLastUsedAt(now);
        if (!refreshToken.getDeviceType().shouldRotateRefreshToken()) {
            return new RefreshTokenUse(refreshTokenRepository.save(refreshToken), rawToken);
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        IssuedRefreshToken successor = issueRefreshToken(
                refreshToken.getUser(), refreshToken.getDeviceType(), refreshToken.getFamilyId());
        return new RefreshTokenUse(successor.refreshToken(), successor.rawToken());
    }

    /** @deprecated Use {@link #useRefreshToken(String)} for refresh requests. */
    @Deprecated
    @Transactional
    public RefreshToken verifyAndGetRefreshToken(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(RefreshTokenNotFoundException::new);
        Instant now = clock.instant();
        if (refreshToken.isRevoked()) {
            throw new RefreshTokenRevokedException();
        }
        if (refreshToken.isExpired(now)) {
            throw new RefreshTokenExpiredException();
        }
        refreshToken.setLastUsedAt(now);
        refreshToken.setRawToken(rawToken);
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeRefreshToken(String token){
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token).orElseThrow(RefreshTokenNotFoundException::new);
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Transactional
    public void revokeAllUserWebTokens(UUID userId) {
        List<DeviceType> webTypes = Stream.of(DeviceType.values())
                .filter(DeviceType::shouldRotateRefreshToken)
                .toList();
        refreshTokenRepository.revokeAllByUserIdAndDeviceTypeIn(userId, webTypes);
    }

    @Transactional
    public void revokeAllUserMobileTokens(UUID userId) {
        List<DeviceType> mobileTypes = Stream.of(DeviceType.values())
                .filter(deviceType -> !deviceType.shouldRotateRefreshToken())
                .toList();
        refreshTokenRepository.revokeAllByUserIdAndDeviceTypeIn(userId, mobileTypes);
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(clock.instant());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
