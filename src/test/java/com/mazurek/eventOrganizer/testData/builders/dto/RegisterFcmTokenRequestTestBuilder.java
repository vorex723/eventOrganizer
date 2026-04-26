package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import com.mazurek.eventOrganizer.user.dto.RegisterFcmTokenRequest;

public class RegisterFcmTokenRequestTestBuilder {

    private String token = UserConstants.FIRST_USER_FCM_TOKEN;

    public static RegisterFcmTokenRequestTestBuilder firstUserToken() {
        return new RegisterFcmTokenRequestTestBuilder()
                .token(UserConstants.FIRST_USER_FCM_TOKEN);
    }

    public static RegisterFcmTokenRequestTestBuilder updatedFirstUserToken() {
        return new RegisterFcmTokenRequestTestBuilder()
                .token(UserConstants.FIRST_USER_NEW_FCM_TOKEN);
    }

    public RegisterFcmTokenRequestTestBuilder token(String token) {
        this.token = token;
        return this;
    }

    public RegisterFcmTokenRequest build() {
        return new RegisterFcmTokenRequest(token);
    }
}
