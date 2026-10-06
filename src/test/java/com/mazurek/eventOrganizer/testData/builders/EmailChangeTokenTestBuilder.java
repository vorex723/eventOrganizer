package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.auth.EmailChangeToken;
import com.mazurek.eventOrganizer.user.User;
import java.time.Instant;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.TokenFixtureConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class EmailChangeTokenTestBuilder {
    private Long id = TokenFixtureConstants.TOKEN_ID;
    private String tokenHash = TokenFixtureConstants.TOKEN_HASH;
    private String pendingEmail = UserConstants.FIRST_USER_NEW_EMAIL;
    private User user;
    private boolean userSet;
    private Instant expirationDate = TokenFixtureConstants.EXPIRATION_DATE;

    public EmailChangeTokenTestBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public EmailChangeTokenTestBuilder tokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
        return this;
    }

    public EmailChangeTokenTestBuilder pendingEmail(String pendingEmail) {
        this.pendingEmail = pendingEmail;
        return this;
    }

    public EmailChangeTokenTestBuilder user(User user) {
        this.user = user;
        this.userSet = true;
        return this;
    }

    public EmailChangeTokenTestBuilder expirationDate(Instant expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }

    public EmailChangeTokenTestBuilder unissued() {
        tokenHash = null;
        expirationDate = null;
        pendingEmail = null;
        return this;
    }

    public EmailChangeTokenTestBuilder rawToken(UUID rawToken) {
        tokenHash = rawToken == null ? null : RefreshTokenTestBuilder.hashOf(rawToken.toString());
        return this;
    }

    public EmailChangeToken build() {
        EmailChangeToken value = new EmailChangeToken();
        value.setId(id);
        value.setTokenHash(tokenHash);
        value.setPendingEmail(pendingEmail);
        value.setUser(userSet ? user : UserTestBuilder.firstUser().build());
        value.setExpirationDate(expirationDate);
        return value;
    }
}
