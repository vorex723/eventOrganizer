package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.jwt.RefreshToken;
import com.mazurek.eventOrganizer.jwt.RefreshTokenUse;

import static com.mazurek.eventOrganizer.testData.TestConstants.RefreshTokenConstants;

public class RefreshTokenUseTestBuilder {
    private String rawToken = RefreshTokenConstants.FIRST_REFRESH_TOKEN;
    private RefreshToken refreshToken;
    private boolean refreshTokenSet;

    public RefreshTokenUseTestBuilder rawToken(String rawToken) {
        this.rawToken = rawToken;
        return this;
    }

    public RefreshTokenUseTestBuilder refreshToken(RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
        refreshTokenSet = true;
        return this;
    }

    public RefreshTokenUse build() {
        RefreshToken token = refreshTokenSet ? refreshToken
                : new IssuedRefreshTokenTestBuilder().rawToken(rawToken).build().refreshToken();
        return new RefreshTokenUse(token, rawToken);
    }
}
