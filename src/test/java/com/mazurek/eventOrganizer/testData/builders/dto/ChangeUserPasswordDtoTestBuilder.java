package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import com.mazurek.eventOrganizer.user.dto.ChangeUserPasswordDto;

public class ChangeUserPasswordDtoTestBuilder {

    private String newPassword = UserConstants.NEW_PASSWORD;
    private String newPasswordConfirmation = UserConstants.NEW_PASSWORD;
    private String password = UserConstants.USER_PASSWORD;

    public static ChangeUserPasswordDtoTestBuilder validChange() {
        return new ChangeUserPasswordDtoTestBuilder();
    }

    public ChangeUserPasswordDtoTestBuilder newPassword(String newPassword) {
        this.newPassword = newPassword;
        return this;
    }

    public ChangeUserPasswordDtoTestBuilder newPasswordConfirmation(String newPasswordConfirmation) {
        this.newPasswordConfirmation = newPasswordConfirmation;
        return this;
    }

    public ChangeUserPasswordDtoTestBuilder password(String password) {
        this.password = password;
        return this;
    }

    public ChangeUserPasswordDto build() {
        return ChangeUserPasswordDto.builder()
                .newPassword(newPassword)
                .newPasswordConfirmation(newPasswordConfirmation)
                .password(password)
                .build();
    }
}
