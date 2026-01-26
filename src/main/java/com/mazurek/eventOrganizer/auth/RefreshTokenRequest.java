package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.jwt.RefreshToken;

public record RefreshTokenRequest(String refreshToken) {
}
