package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.notification.dto.UpdateNotificationPreferenceDto;
import com.mazurek.eventOrganizer.notification.dto.UpdateNotificationPreferencesDto;
import java.util.ArrayList;
import java.util.List;

import static com.mazurek.eventOrganizer.testData.TestConstants.DeliveryFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class UpdateNotificationPreferencesDtoTestBuilder {
    private Long version = DeliveryFixtureConstants.PREFERENCE_VERSION;
    private List<UpdateNotificationPreferenceDto> preferences;
    private boolean preferencesSet;

    public UpdateNotificationPreferencesDtoTestBuilder version(Long version) {
        this.version = version;
        return this;
    }

    public UpdateNotificationPreferencesDtoTestBuilder preferences(List<UpdateNotificationPreferenceDto> preferences) {
        this.preferences = preferences == null ? null : new ArrayList<>(preferences);
        this.preferencesSet = true;
        return this;
    }


    public UpdateNotificationPreferencesDto build() {
        return new UpdateNotificationPreferencesDto(
                version,
                preferencesSet ? preferences == null ? null : new ArrayList<>(preferences) : List.of(new UpdateNotificationPreferenceDtoTestBuilder().build())
        );
    }
}
