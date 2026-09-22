package com.mazurek.eventOrganizer.jwt;

/** Raw credential is intentionally available only while it is returned to the client. */
public record IssuedRefreshToken(RefreshToken refreshToken, String rawToken) {
}
