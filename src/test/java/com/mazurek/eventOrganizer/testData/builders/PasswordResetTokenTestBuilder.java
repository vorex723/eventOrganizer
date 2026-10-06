package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.auth.PasswordResetToken;
import com.mazurek.eventOrganizer.user.User;
import java.time.Instant;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.TokenFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class PasswordResetTokenTestBuilder {
    private Long id = TokenFixtureConstants.TOKEN_ID;
    private String tokenHash = TokenFixtureConstants.TOKEN_HASH;
    private User user;
    private boolean userSet;
    private Instant expirationDate = TokenFixtureConstants.EXPIRATION_DATE;

    public PasswordResetTokenTestBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public PasswordResetTokenTestBuilder tokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
        return this;
    }

    public PasswordResetTokenTestBuilder user(User user) {
        this.user = user;
        this.userSet = true;
        return this;
    }

    public PasswordResetTokenTestBuilder expirationDate(Instant expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }

    public PasswordResetTokenTestBuilder unissued() {
        tokenHash = null;
        expirationDate = null;
        return this;
    }

    public PasswordResetTokenTestBuilder rawToken(UUID rawToken) {
        tokenHash = rawToken == null ? null : RefreshTokenTestBuilder.hashOf(rawToken.toString());
        return this;
    }

    public PasswordResetToken build() {
        PasswordResetToken value = new PasswordResetToken();
        value.setId(id);
        value.setTokenHash(tokenHash);
        value.setUser(userSet ? user : UserTestBuilder.firstUser().build());
        value.setExpirationDate(expirationDate);
        return value;
    }
}
