package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.user.dto.DeleteCurrentUserDto;

import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class DeleteCurrentUserDtoTestBuilder {
    private String password = UserConstants.USER_PASSWORD;

    public DeleteCurrentUserDtoTestBuilder password(String password) {
        this.password = password;
        return this;
    }


    public DeleteCurrentUserDto build() {
        return new DeleteCurrentUserDto(
                password
        );
    }
}
