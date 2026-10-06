package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.EventTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.TagTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.FileTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ThreadTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ThreadReplyTestBuilder;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import com.mazurek.eventOrganizer.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Event unit tests:")
class EventUnitTest {

    private User firstOwner;
    private User secondOwner;
    private Event event;
    private City cityKrakow;
    private Tag firstTag;

    @BeforeEach
    void setUp() {
        City cityWarsaw = CityTestBuilder.warsaw().build();
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
    @DisplayName("When setting owner should replace the owning-side reference")
    void whenSettingOwnerShouldReplaceOwnerReference() {
        event.setOwner(secondOwner);

        assertThat(event.getOwner()).isEqualTo(secondOwner);
    }

    @Test
    @DisplayName("When adding attendee should update membership and count")
    void whenAddingAttendeeShouldUpdateMembershipAndCount() {
        event.addAttendee(secondOwner);

        assertThat(event.getAttendees()).contains(secondOwner);
        assertThat(event.getAttendeeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("When removing attendee should update membership and count")
    void whenRemovingAttendeeShouldUpdateMembershipAndCount() {
        event.addAttendee(secondOwner);

        event.removeAttendee(secondOwner);

        assertThat(event.getAttendees()).doesNotContain(secondOwner);
        assertThat(event.getAttendeeCount()).isZero();
    }

    @Test
    @DisplayName("When adding tag should update the owning-side collection")
    void whenAddingTagShouldUpdateTags() {
        event.addTag(firstTag);

        assertThat(event.getTags()).contains(firstTag);
    }

    @Test
    @DisplayName("When removing tag should update the owning-side collection")
    void whenRemovingTagShouldUpdateTags() {
        event.addTag(firstTag);

        event.removeTag(firstTag);

        assertThat(event.getTags()).doesNotContain(firstTag);
    }

    @Test
    @DisplayName("When clearing city should clear the owning-side reference")
    void whenClearingCityShouldClearCityReference() {
        event.setCity(null);

        assertThat(event.getCity()).isNull();
    }

    @Test
    @DisplayName("When changing city should replace the owning-side reference")
    void whenChangingCityShouldReplaceCityReference() {
        event.setCity(cityKrakow);

        assertThat(event.getCity()).isEqualTo(cityKrakow);
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
    @DisplayName("When adding distinct transient attendees should keep them in collection")
    void whenAddingDistinctTransientAttendeesShouldKeepThemInCollection() {
        User first = UserTestBuilder.firstUser().id(null).homeCity(null).roles(Set.of()).build();
        User second = UserTestBuilder.secondUser().id(null).homeCity(null).roles(Set.of()).build();
        event.addAttendee(first);
        event.addAttendee(second);
        event.addAttendee(first);

        assertThat(event.getAttendees()).containsExactlyInAnyOrder(first, second);
        assertThat(event.getAttendeeCount()).isEqualTo(2);

        event.removeAttendee(first);
        assertThat(event.getAttendees()).containsExactly(second);
        assertThat(event.getAttendeeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("When adding distinct transient tags should keep them in collection")
    void whenAddingDistinctTransientTagsShouldKeepThemInCollection() {
        Tag first = TagTestBuilder.firstTag().id(null).name("first").build();
        Tag second = TagTestBuilder.secondTag().id(null).name("second").build();
        event.addTag(first);
        event.addTag(second);

        assertThat(event.getTags()).containsExactlyInAnyOrder(first, second);

        event.removeTag(first);
        assertThat(event.getTags()).containsExactly(second);
    }

    @Test
    @DisplayName("When assigning distinct transient events and cities should keep their associations separate")
    void whenAssigningDistinctTransientEventsAndCitiesShouldKeepTheirAssociationsSeparate() {
        City firstCity = com.mazurek.eventOrganizer.testData.builders.CityTestBuilder.warsaw().name("first").id(null).build();
        City secondCity = com.mazurek.eventOrganizer.testData.builders.CityTestBuilder.warsaw().name("second").id(null).build();
        User owner = UserTestBuilder.firstUser().id(null).homeCity(null).roles(Set.of()).build();
        Event first = EventTestBuilder.firstEvent().id(null).city(null).owner(null).build();
        Event second = EventTestBuilder.secondEvent().id(null).city(null).owner(null).build();
        first.setOwner(owner);
        second.setOwner(owner);
        first.setCity(firstCity);
        second.setCity(firstCity);

        first.setCity(secondCity);
        first.setOwner(null);
        assertThat(first.getOwner()).isNull();
        assertThat(first.getCity()).isSameAs(secondCity);
        assertThat(second.getOwner()).isSameAs(owner);
        assertThat(second.getCity()).isSameAs(firstCity);
    }

    @Test
    @DisplayName("When adding distinct transient threads and replies should keep them in collections")
    void whenAddingDistinctTransientThreadsAndRepliesShouldKeepThemInCollections() {
        Thread first = ThreadTestBuilder.firstThread().id(null).event(null).owner(null).build();
        Thread second = ThreadTestBuilder.secondThread().id(null).event(null).owner(null).build();
        event.addThread(first);
        event.addThread(second);
        first.setOwner(firstOwner);
        second.setOwner(firstOwner);
        assertThat(event.getThreads()).containsExactlyInAnyOrder(first, second);
        assertThat(first.getOwner()).isSameAs(firstOwner);
        assertThat(second.getOwner()).isSameAs(firstOwner);

        ThreadReply firstReply = ThreadReplyTestBuilder.firstReply().id(null).thread(null).replier(null).build();
        ThreadReply secondReply = ThreadReplyTestBuilder.secondReply().id(null).thread(null).replier(null).build();
        first.addReplyToThread(firstReply);
        first.addReplyToThread(secondReply);
        firstReply.setReplier(firstOwner);
        secondReply.setReplier(firstOwner);
        assertThat(first.getReplies()).containsExactlyInAnyOrder(firstReply, secondReply);
        assertThat(firstReply.getReplier()).isSameAs(firstOwner);
        assertThat(secondReply.getReplier()).isSameAs(firstOwner);

        first.removeReply(firstReply);
        second.addReplyToThread(firstReply);
        firstReply.setReplier(null);
        assertThat(first.getReplies()).containsExactly(secondReply);
        assertThat(second.getReplies()).containsExactly(firstReply);
        assertThat(firstReply.getReplier()).isNull();
        assertThat(secondReply.getReplier()).isSameAs(firstOwner);
    }

    @Test
    @DisplayName("When adding distinct transient files should keep them in collection")
    void whenAddingDistinctTransientFilesShouldKeepThemInCollection() {
        File first = FileTestBuilder.jpgFile().id(null).event(null).owner(null).build();
        File second = FileTestBuilder.jpegFile().id(null).event(null).owner(null).build();
        event.addFile(first);
        event.addFile(second);
        first.setOwner(firstOwner);
        second.setOwner(firstOwner);
        assertThat(event.getFiles()).containsExactlyInAnyOrder(first, second);
        assertThat(first.getOwner()).isSameAs(firstOwner);
        assertThat(second.getOwner()).isSameAs(firstOwner);

        event.removeFile(first);
        first.setOwner(null);
        assertThat(event.getFiles()).containsExactly(second);
        assertThat(first.getOwner()).isNull();
        assertThat(second.getOwner()).isSameAs(firstOwner);
    }

    @Test
    @DisplayName("When replacing tags should preserve collection instance")
    void whenReplacingTagsShouldPreserveCollectionInstance() {
        Tag secondTag = TagTestBuilder.secondTag().id(null).name("second").build();
        event.addTag(firstTag);
        Set<Tag> originalTags = event.getTags();

        event.setTags(Set.of(secondTag));
        assertThat(event.getTags()).isSameAs(originalTags).containsExactly(secondTag);

        event.setTags(event.getTags());
        assertThat(event.getTags()).isSameAs(originalTags).containsExactly(secondTag);

        event.setTags(null);
        assertThat(event.getTags()).isSameAs(originalTags).isEmpty();
    }
}
