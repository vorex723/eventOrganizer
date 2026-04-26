package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.jwt.RefreshToken;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.testData.TestConstants.*;

import java.time.Instant;

public class RefreshTokenTestBuilder {

    private Long id = RefreshTokenConstants.FIRST_REFRESH_TOKEN_ID;
    private String token = RefreshTokenConstants.FIRST_REFRESH_TOKEN;
    private User user = UserTestBuilder.firstUser().build();
    private Instant expiryDate = RefreshTokenConstants.FIRST_REFRESH_TOKEN_EXPIRY_DATE;
    private boolean revoked = RefreshTokenConstants.REFRESH_TOKEN_REVOKED_FALSE;
    private DeviceType deviceType = RefreshTokenConstants.FIRST_REFRESH_TOKEN_DEVICE_TYPE;
    private String deviceInfo = RefreshTokenConstants.FIRST_REFRESH_TOKEN_DEVICE_INFO;
    private Instant createdAt = RefreshTokenConstants.FIRST_REFRESH_TOKEN_CREATED_AT;
    private Instant lastUsedAt = RefreshTokenConstants.FIRST_REFRESH_TOKEN_LAST_USED_AT;

    public static RefreshTokenTestBuilder firstRefreshToken() {
        return new RefreshTokenTestBuilder();
    }

    public static RefreshTokenTestBuilder secondRefreshToken() {
        return new RefreshTokenTestBuilder()
                .id(RefreshTokenConstants.SECOND_REFRESH_TOKEN_ID)
                .token(RefreshTokenConstants.SECOND_REFRESH_TOKEN)
                .deviceType(RefreshTokenConstants.SECOND_REFRESH_TOKEN_DEVICE_TYPE)
                .deviceInfo(RefreshTokenConstants.SECOND_REFRESH_TOKEN_DEVICE_INFO);
    }

    public static RefreshTokenTestBuilder expiredRefreshToken() {
        return new RefreshTokenTestBuilder()
                .id(RefreshTokenConstants.THIRD_REFRESH_TOKEN_ID)
                .token(RefreshTokenConstants.EXPIRED_REFRESH_TOKEN)
                .deviceType(RefreshTokenConstants.THIRD_REFRESH_TOKEN_DEVICE_TYPE)
                .deviceInfo(RefreshTokenConstants.THIRD_REFRESH_TOKEN_DEVICE_INFO)
                .createdAt(RefreshTokenConstants.EXPIRED_REFRESH_TOKEN_CREATED_AT)
                .lastUsedAt(RefreshTokenConstants.EXPIRED_REFRESH_TOKEN_LAST_USED_AT)
                .expiryDate(RefreshTokenConstants.EXPIRED_REFRESH_TOKEN_EXPIRY_DATE)
                .revoked(false);
    }

    public static RefreshTokenTestBuilder revokedRefreshToken() {
        return new RefreshTokenTestBuilder()
                .id(RefreshTokenConstants.THIRD_REFRESH_TOKEN_ID)
                .token(RefreshTokenConstants.REVOKED_REFRESH_TOKEN)
                .revoked(RefreshTokenConstants.REFRESH_TOKEN_REVOKED_TRUE);
    }

    public static RefreshTokenTestBuilder firstRefreshTokenForUser(User user) {
        return firstRefreshToken().user(user);
    }

    public static RefreshTokenTestBuilder secondRefreshTokenForUser(User user) {
        return secondRefreshToken().user(user);
    }

    public static RefreshTokenTestBuilder expiredRefreshTokenForUser(User user) {
        return expiredRefreshToken().user(user);
    }

    public static RefreshTokenTestBuilder revokedRefreshTokenForUser(User user) {
        return revokedRefreshToken().user(user);
    }

    public RefreshTokenTestBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public RefreshTokenTestBuilder token(String token) {
        this.token = token;
        return this;
    }

    public RefreshTokenTestBuilder user(User user) {
        this.user = user;
        return this;
    }

    public RefreshTokenTestBuilder expiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
        return this;
    }

    public RefreshTokenTestBuilder revoked(boolean revoked) {
        this.revoked = revoked;
        return this;
    }

    public RefreshTokenTestBuilder deviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
        return this;
    }

    public RefreshTokenTestBuilder deviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
        return this;
    }

    public RefreshTokenTestBuilder createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public RefreshTokenTestBuilder lastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
        return this;
    }

    public RefreshToken build() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(id);
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(expiryDate);
        refreshToken.setRevoked(revoked);
        refreshToken.setDeviceType(deviceType);
        refreshToken.setDeviceInfo(deviceInfo);
        refreshToken.setCreatedAt(createdAt);
        refreshToken.setLastUsedAt(lastUsedAt);
        return refreshToken;
    }
}
