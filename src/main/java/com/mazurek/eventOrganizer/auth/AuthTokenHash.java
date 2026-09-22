package com.mazurek.eventOrganizer.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

final class AuthTokenHash {

    private AuthTokenHash() {
    }

    static String sha256(UUID token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available.", exception);
        }
    }
}
