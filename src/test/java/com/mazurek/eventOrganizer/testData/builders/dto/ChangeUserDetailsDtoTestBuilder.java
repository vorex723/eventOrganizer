package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.testData.TestConstants.CitiesConstants;
import com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import com.mazurek.eventOrganizer.user.dto.ChangeUserDetailsDto;

public class ChangeUserDetailsDtoTestBuilder {

    private String firstName = UserConstants.SECOND_USER_FIRST_NAME;
    private String lastName = UserConstants.SECOND_USER_LAST_NAME;
    private String homeCity = CitiesConstants.KRAKOW_NAME;

    public static ChangeUserDetailsDtoTestBuilder validUpdate() {
        return new ChangeUserDetailsDtoTestBuilder();
    }

    public ChangeUserDetailsDtoTestBuilder firstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public ChangeUserDetailsDtoTestBuilder lastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public ChangeUserDetailsDtoTestBuilder homeCity(String homeCity) {
        this.homeCity = homeCity;
        return this;
    }

    public ChangeUserDetailsDto build() {
        return ChangeUserDetailsDto.builder()
                .firstName(firstName)
                .lastName(lastName)
                .homeCity(homeCity)
                .build();
    }
}
