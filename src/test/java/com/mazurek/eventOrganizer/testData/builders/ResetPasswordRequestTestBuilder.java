package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.auth.dto.ResetPasswordRequest;

import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class ResetPasswordRequestTestBuilder {
    private String password = UserConstants.NEW_PASSWORD;
    private String passwordConfirmation = UserConstants.NEW_PASSWORD;

    public ResetPasswordRequestTestBuilder password(String password) {
        this.password = password;
        return this;
    }

    public ResetPasswordRequestTestBuilder passwordConfirmation(String passwordConfirmation) {
        this.passwordConfirmation = passwordConfirmation;
        return this;
    }


    public ResetPasswordRequest build() {
        return new ResetPasswordRequest(
                password,
                passwordConfirmation
        );
    }
}
