package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.config.properties.JwtProperties;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenExpiredException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenRevokedException;
import com.mazurek.eventOrganizer.user.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Transactional
    public RefreshToken createRefreshToken(User user, DeviceType deviceType){

        Long expiration = deviceType.shouldRotateRefreshToken() ? jwtProperties.getRefreshShortExpiration() : jwtProperties.getRefreshLongExpiration();
        Instant tokenCreateDate = clock.instant();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setDeviceType(deviceType);
        refreshToken.setCreatedAt(tokenCreateDate);
        refreshToken.setExpiryDate(tokenCreateDate.plusMillis(expiration));
        refreshToken.setLastUsedAt(tokenCreateDate);

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken verifyAndGetRefreshToken(String token){

        RefreshToken  refreshToken = refreshTokenRepository.findByToken(token).orElseThrow(RefreshTokenNotFoundException::new);
        refreshToken.setLastUsedAt(clock.instant());
        refreshTokenRepository.save(refreshToken);

        if (refreshToken.isRevoked())
            throw new RefreshTokenRevokedException();
        if (clock.instant().isAfter(refreshToken.getExpiryDate()))
            throw new RefreshTokenExpiredException();

        return refreshToken;
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

}
