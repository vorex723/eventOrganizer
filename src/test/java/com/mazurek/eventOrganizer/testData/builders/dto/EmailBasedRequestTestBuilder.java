package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.auth.dto.EmailBasedRequest;

import static com.mazurek.eventOrganizer.testData.TestConstants.InvalidInputConstants.INVALID_EMAIL;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.NOT_EXISTING_USER_EMAIL;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.THIRD_USER_EMAIL;

public class EmailBasedRequestTestBuilder {

    private String email = FIRST_USER_EMAIL;

    public static EmailBasedRequestTestBuilder firstUser() {
        return new EmailBasedRequestTestBuilder()
                .email(FIRST_USER_EMAIL);
    }

    public static EmailBasedRequestTestBuilder thirdUser() {
        return new EmailBasedRequestTestBuilder()
                .email(THIRD_USER_EMAIL);
    }

    public static EmailBasedRequestTestBuilder nonExistingUser() {
        return new EmailBasedRequestTestBuilder()
                .email(NOT_EXISTING_USER_EMAIL);
    }

    public static EmailBasedRequestTestBuilder invalidEmail() {
        return new EmailBasedRequestTestBuilder()
                .email(INVALID_EMAIL);
    }

    public EmailBasedRequestTestBuilder email(String email) {
        this.email = email;
        return this;
    }

    public EmailBasedRequest build() {
        return new EmailBasedRequest(email);
    }
}
