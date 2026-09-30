package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.EventTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.TagTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import com.mazurek.eventOrganizer.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
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
    @DisplayName("When adding attendee should keep both sides of attendance relationship in sync")
    void whenAddingAttendeeShouldKeepBothSidesOfAttendanceRelationshipInSync() {
        event.addAttendee(secondOwner);

        assertThat(event.getAttendees()).contains(secondOwner);
        assertThat(event.getAttendeeCount()).isEqualTo(1);
        assertThat(secondOwner.getAttendingEvents()).contains(event);
    }

    @Test
    @DisplayName("When removing attendee should keep both sides of attendance relationship in sync")
    void whenRemovingAttendeeShouldKeepBothSidesOfAttendanceRelationshipInSync() {
        event.addAttendee(secondOwner);

        event.removeAttendee(secondOwner);

        assertThat(event.getAttendees()).doesNotContain(secondOwner);
        assertThat(event.getAttendeeCount()).isZero();
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

    @Test
    @DisplayName("When checking if event had place should compare start date with provided time")
    void whenCheckingIfEventHadPlaceShouldCompareStartDateWithProvidedTime() {
        event.setEventStartDate(TimeConstants.ONE_HOUR_AGO);

        assertThat(event.hadPlace(TimeConstants.NOW)).isTrue();
    }

    @Test
    @DisplayName("When checking if event had place should return false for future event")
    void whenCheckingIfEventHadPlaceShouldReturnFalseForFutureEvent() {
        event.setEventStartDate(TimeConstants.ONE_WEEK_FROM_NOW);

        assertThat(event.hadPlace(TimeConstants.NOW)).isFalse();
    }

    @Test
    @DisplayName("Distinct transient attendees should not collapse and removal should preserve the other attendee")
    void shouldKeepDistinctTransientAttendees() {
        User first = new User();
        User second = new User();
        event.addAttendee(first);
        event.addAttendee(second);
        event.addAttendee(first);

        assertThat(event.getAttendees()).containsExactlyInAnyOrder(first, second);
        assertThat(event.getAttendeeCount()).isEqualTo(2);
        assertThat(first.getAttendingEvents()).containsExactly(event);
        assertThat(second.getAttendingEvents()).containsExactly(event);

        event.removeAttendee(first);
        assertThat(event.getAttendees()).containsExactly(second);
        assertThat(event.getAttendeeCount()).isEqualTo(1);
        assertThat(first.getAttendingEvents()).isEmpty();
        assertThat(second.getAttendingEvents()).containsExactly(event);
    }

    @Test
    @DisplayName("Distinct transient tags should remain synchronized on both sides")
    void shouldKeepDistinctTransientTags() {
        Tag first = new Tag("first");
        Tag second = new Tag("second");
        event.addTag(first);
        event.addTag(second);

        assertThat(event.getTags()).containsExactlyInAnyOrder(first, second);
        assertThat(first.getEvents()).containsExactly(event);
        assertThat(second.getEvents()).containsExactly(event);

        event.removeTag(first);
        assertThat(event.getTags()).containsExactly(second);
        assertThat(first.getEvents()).isEmpty();
        assertThat(second.getEvents()).containsExactly(event);
    }

    @Test
    @DisplayName("Distinct transient events should remain in owner and city collections when another is removed")
    void shouldKeepDistinctTransientEventsAndCities() {
        City firstCity = new City("first");
        City secondCity = new City("second");
        User owner = new User();
        Event first = new Event();
        Event second = new Event();
        first.setOwner(owner);
        second.setOwner(owner);
        first.setCity(firstCity);
        second.setCity(firstCity);

        assertThat(owner.getUserEvents()).containsExactlyInAnyOrder(first, second);
        assertThat(firstCity.getEvents()).containsExactlyInAnyOrder(first, second);

        first.setCity(secondCity);
        first.setOwner(null);
        assertThat(owner.getUserEvents()).containsExactly(second);
        assertThat(firstCity.getEvents()).containsExactly(second);
        assertThat(secondCity.getEvents()).containsExactly(first);
    }

    @Test
    @DisplayName("Distinct transient threads and replies should not collapse in relationship sets")
    void shouldKeepDistinctTransientThreadsAndReplies() {
        Thread first = new Thread();
        Thread second = new Thread();
        first.setEvent(event);
        second.setEvent(event);
        first.setOwner(firstOwner);
        second.setOwner(firstOwner);
        assertThat(event.getThreads()).containsExactlyInAnyOrder(first, second);
        assertThat(firstOwner.getThreads()).containsExactlyInAnyOrder(first, second);

        ThreadReply firstReply = new ThreadReply();
        ThreadReply secondReply = new ThreadReply();
        firstReply.setThread(first);
        secondReply.setThread(first);
        firstReply.setReplier(firstOwner);
        secondReply.setReplier(firstOwner);
        assertThat(first.getReplies()).containsExactlyInAnyOrder(firstReply, secondReply);
        assertThat(firstOwner.getThreadReplies()).containsExactlyInAnyOrder(firstReply, secondReply);

        firstReply.setThread(second);
        firstReply.setReplier(null);
        assertThat(first.getReplies()).containsExactly(secondReply);
        assertThat(second.getReplies()).containsExactly(firstReply);
        assertThat(firstOwner.getThreadReplies()).containsExactly(secondReply);
    }

    @Test
    @DisplayName("Distinct transient files should not collapse in event and owner collections")
    void shouldKeepDistinctTransientFiles() {
        File first = new File();
        File second = new File();
        first.setEvent(event);
        second.setEvent(event);
        first.setOwner(firstOwner);
        second.setOwner(firstOwner);
        assertThat(event.getFiles()).containsExactlyInAnyOrder(first, second);
        assertThat(firstOwner.getFiles()).containsExactlyInAnyOrder(first, second);

        first.setEvent(null);
        first.setOwner(null);
        assertThat(event.getFiles()).containsExactly(second);
        assertThat(firstOwner.getFiles()).containsExactly(second);
    }
}
