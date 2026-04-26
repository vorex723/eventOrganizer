package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import com.mazurek.eventOrganizer.user.dto.ChangeUserEmailDto;

public class ChangeUserEmailDtoTestBuilder {

    private String newEmail = UserConstants.FIRST_USER_NEW_EMAIL;
    private String newEmailConfirmation = UserConstants.FIRST_USER_NEW_EMAIL;
    private String password = UserConstants.USER_PASSWORD;

    public static ChangeUserEmailDtoTestBuilder validChange() {
        return new ChangeUserEmailDtoTestBuilder();
    }

    public ChangeUserEmailDtoTestBuilder newEmail(String newEmail) {
        this.newEmail = newEmail;
        return this;
    }

    public ChangeUserEmailDtoTestBuilder newEmailConfirmation(String newEmailConfirmation) {
        this.newEmailConfirmation = newEmailConfirmation;
        return this;
    }

    public ChangeUserEmailDtoTestBuilder password(String password) {
        this.password = password;
        return this;
    }

    public ChangeUserEmailDto build() {
        return ChangeUserEmailDto.builder()
                .newEmail(newEmail)
                .newEmailConfirmation(newEmailConfirmation)
                .password(password)
                .build();
    }
}
