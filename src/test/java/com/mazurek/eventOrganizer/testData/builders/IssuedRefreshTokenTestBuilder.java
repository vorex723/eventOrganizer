package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.jwt.IssuedRefreshToken;
import com.mazurek.eventOrganizer.jwt.RefreshToken;

import static com.mazurek.eventOrganizer.testData.TestConstants.RefreshTokenConstants;

/** Uses the existing token fixture factory; explicit relationships are not repaired. */
public class IssuedRefreshTokenTestBuilder {
    private String rawToken = RefreshTokenConstants.FIRST_REFRESH_TOKEN;
    private RefreshToken refreshToken;
    private boolean refreshTokenSet;

    public IssuedRefreshTokenTestBuilder rawToken(String rawToken) {
        this.rawToken = rawToken;
        return this;
    }

    public IssuedRefreshTokenTestBuilder refreshToken(RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
        refreshTokenSet = true;
        return this;
    }

    public IssuedRefreshToken build() {
        return refreshTokenSet
                ? new IssuedRefreshToken(refreshToken, rawToken)
                : RefreshTokenTestBuilder.firstRefreshToken().rawToken(rawToken).buildIssued();
    }
}
