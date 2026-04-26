package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.auth.ActivationToken;
import com.mazurek.eventOrganizer.user.User;

import java.time.Instant;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

public class ActivationTokenTestBuilder {

    private Long id = ActivationTokenConstants.FIRST_ACTIVATION_TOKEN_ID;
    private UUID token = ActivationTokenConstants.FIRST_ACTIVATION_TOKEN_UUID;
    private Instant expirationDate = TimeConstants.NOW.plusMillis(ActivationTokenConstants.ACTIVATION_TOKEN_EXPIRATION_SECONDS);
    private User user = UserTestBuilder.firstUser().build();

    public static ActivationTokenTestBuilder firstToken() {
        return new ActivationTokenTestBuilder()
                .id(ActivationTokenConstants.FIRST_ACTIVATION_TOKEN_ID)
                .token(ActivationTokenConstants.FIRST_ACTIVATION_TOKEN_UUID)
                .expirationDate(TimeConstants.NOW.plusMillis(ActivationTokenConstants.ACTIVATION_TOKEN_EXPIRATION_SECONDS));
    }

    public static ActivationTokenTestBuilder secondToken() {
        return new ActivationTokenTestBuilder()
                .id(ActivationTokenConstants.SECOND_ACTIVATION_TOKEN_ID)
                .token(ActivationTokenConstants.SECOND_ACTIVATION_TOKEN_UUID)
                .expirationDate(TimeConstants.NOW.plusMillis(ActivationTokenConstants.ACTIVATION_TOKEN_EXPIRATION_SECONDS));
    }

    public static ActivationTokenTestBuilder expiredTokenForUser(User user) {
        return new ActivationTokenTestBuilder()
                .id(ActivationTokenConstants.FIRST_ACTIVATION_TOKEN_ID)
                .token(ActivationTokenConstants.FIRST_ACTIVATION_TOKEN_UUID)
                .user(user)
                .expirationDate(TimeConstants.ONE_WEEK_AGO);
    }

    public ActivationTokenTestBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public ActivationTokenTestBuilder token(UUID token) {
        this.token = token;
        return this;
    }

    public ActivationTokenTestBuilder user(User user) {
        this.user = user;
        return this;
    }

    public ActivationTokenTestBuilder expirationDate(Instant expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }

    public ActivationToken build() {
        return ActivationToken.builder()
                .id(id)
                .token(token)
                .user(user)
                .expirationDate(expirationDate)
                .build();
    }
}
