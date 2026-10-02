package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.testData.TestConstants.CitiesConstants;
import com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import com.mazurek.eventOrganizer.user.dto.ChangeUserDetailsDto;

public class ChangeUserDetailsDtoTestBuilder {

    private String firstName = UserConstants.SECOND_USER_FIRST_NAME;
    private String lastName = UserConstants.SECOND_USER_LAST_NAME;
    private String homeCityExternalId = CitiesConstants.KRAKOW_NAME;
    private String timeZone = UserConstants.SECOND_USER_TIMEZONE;

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

    public ChangeUserDetailsDtoTestBuilder homeCityExternalId(String homeCityExternalId) {
        this.homeCityExternalId = homeCityExternalId;
        return this;
    }

    public ChangeUserDetailsDtoTestBuilder timeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public ChangeUserDetailsDto build() {
        return ChangeUserDetailsDto.builder()
                .firstName(firstName)
                .lastName(lastName)
                .homeCityExternalId(com.mazurek.eventOrganizer.testData.TestCityData.externalId(homeCityExternalId))
                .timeZone(timeZone)
                .build();
    }
}
