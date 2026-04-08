package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.auth.dto.RefreshTokenRequest;
import com.mazurek.eventOrganizer.testData.TestConstants.RefreshTokenConstants;

public class RefreshTokenRequestTestBuilder {

    private String refreshToken = RefreshTokenConstants.FIRST_REFRESH_TOKEN;

    public static RefreshTokenRequestTestBuilder firstToken() {
        return new RefreshTokenRequestTestBuilder()
                .refreshToken(RefreshTokenConstants.FIRST_REFRESH_TOKEN);
    }

    public static RefreshTokenRequestTestBuilder secondToken() {
        return new RefreshTokenRequestTestBuilder()
                .refreshToken(RefreshTokenConstants.SECOND_REFRESH_TOKEN);
    }

    public RefreshTokenRequestTestBuilder refreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    public RefreshTokenRequest build() {
        return new RefreshTokenRequest(refreshToken);
    }
}
