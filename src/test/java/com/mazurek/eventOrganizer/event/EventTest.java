package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.EventTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.TagTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Event domain tests:")
class EventTest {

    private User firstOwner;
    private User secondOwner;
    private Event event;
    private City cityWarsaw;
    private City cityKrakow;
    private Tag firstTag;

    @BeforeEach
    void setUp() {
        cityWarsaw = CityTestBuilder.warsaw().build();
        cityKrakow = CityTestBuilder.krakow().build();
        firstOwner = UserTestBuilder.firstUser().homeCity(cityWarsaw).build();
        secondOwner = UserTestBuilder.secondUser().homeCity(cityKrakow).build();
        firstTag = TagTestBuilder.firstTag().build();
        event = EventTestBuilder.firstEvent()
                .owner(firstOwner)
                .city(cityWarsaw)
                .build();
    }

    @Test
    @DisplayName("When setting owner should keep both sides of owner relationship in sync")
    void whenSettingOwnerShouldKeepBothSidesOfOwnerRelationshipInSync() {
        event.setOwner(secondOwner);

        assertThat(event.getOwner()).isEqualTo(secondOwner);
        assertThat(secondOwner.getUserEvents()).contains(event);
        assertThat(firstOwner.getUserEvents()).doesNotContain(event);
    }

    @Test
    @DisplayName("When adding attending user should keep both sides of attendance relationship in sync")
    void whenAddingAttendingUserShouldKeepBothSidesOfAttendanceRelationshipInSync() {
        event.addAttendingUser(secondOwner);

        assertThat(event.getAttendingUsers()).contains(secondOwner);
        assertThat(secondOwner.getAttendingEvents()).contains(event);
    }

    @Test
    @DisplayName("When removing attending user should keep both sides of attendance relationship in sync")
    void whenRemovingAttendingUserShouldKeepBothSidesOfAttendanceRelationshipInSync() {
        event.addAttendingUser(secondOwner);

        event.removeAttendingUser(secondOwner);

        assertThat(event.getAttendingUsers()).doesNotContain(secondOwner);
        assertThat(secondOwner.getAttendingEvents()).doesNotContain(event);
    }

    @Test
    @DisplayName("When adding tag should keep both sides of tag relationship in sync")
    void whenAddingTagShouldKeepBothSidesOfTagRelationshipInSync() {
        event.addTag(firstTag);

        assertThat(event.getTags()).contains(firstTag);
        assertThat(firstTag.getEvents()).contains(event);
    }

    @Test
    @DisplayName("When removing tag should keep both sides of tag relationship in sync")
    void whenRemovingTagShouldKeepBothSidesOfTagRelationshipInSync() {
        event.addTag(firstTag);

        event.removeTag(firstTag);

        assertThat(event.getTags()).doesNotContain(firstTag);
        assertThat(firstTag.getEvents()).doesNotContain(event);
    }

    @Test
    @DisplayName("When clearing city should remove event from previous city")
    void whenClearingCityShouldRemoveEventFromPreviousCity() {
        event.setCity(null);

        assertThat(event.getCity()).isNull();
        assertThat(cityWarsaw.getEvents()).doesNotContain(event);
    }

    @Test
    @DisplayName("When changing city should move event between city collections")
    void whenChangingCityShouldMoveEventBetweenCityCollections() {
        event.setCity(cityKrakow);

        assertThat(event.getCity()).isEqualTo(cityKrakow);
        assertThat(cityKrakow.getEvents()).contains(event);
        assertThat(cityWarsaw.getEvents()).doesNotContain(event);
    }
}
