package com.mazurek.eventOrganizer.jwt;

public record RefreshTokenUse(RefreshToken refreshToken, String rawToken) {
}
