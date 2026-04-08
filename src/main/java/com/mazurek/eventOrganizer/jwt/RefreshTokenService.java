package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.config.properties.JwtProperties;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenExpiredException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenRevokedException;
import com.mazurek.eventOrganizer.user.User;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Long shortRefreshTokenExpiration;
    private final Long longRefreshTokenExpiration;
    private final Clock clock;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               JwtProperties jwtProperties,
                               Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.shortRefreshTokenExpiration = jwtProperties.getRefreshShortExpiration();
        this.longRefreshTokenExpiration = jwtProperties.getRefreshLongExpiration();
        this.clock = clock;
    }

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               Long shortRefreshTokenExpiration,
                               Long longRefreshTokenExpiration) {
        this(refreshTokenRepository, shortRefreshTokenExpiration, longRefreshTokenExpiration, Clock.systemUTC());
    }

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               Long shortRefreshTokenExpiration,
                               Long longRefreshTokenExpiration,
                               Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.shortRefreshTokenExpiration = shortRefreshTokenExpiration;
        this.longRefreshTokenExpiration = longRefreshTokenExpiration;
        this.clock = clock;
    }

    @Transactional
    public RefreshToken createRefreshToken(User user, DeviceType deviceType){

        Long expiration = deviceType.shouldRotateRefreshToken() ? shortRefreshTokenExpiration : longRefreshTokenExpiration;
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
