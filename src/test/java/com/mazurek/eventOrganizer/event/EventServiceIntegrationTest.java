package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.city.CityNotFoundException;
import com.mazurek.eventOrganizer.exception.event.*;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.dto.EventCreateDtoTestBuilder;

import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;


import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("EventService integration tests:")
public class EventServiceIntegrationTest {


    private EventCreateDto eventUpdateDto;

    @Autowired
    private EventService eventService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DeletionService deletionService;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private TestDataInitializer testDataInitializer;


    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
    }

    private Set<String> findTagNamesByEventId(UUID eventId) {
        return new HashSet<>(jdbcTemplate.queryForList("""
                select t.name
                from tags t
                join event_tag et on t.id = et.tag_id
                where et.event_id = ?
                """, String.class, eventId));
    }

    private boolean eventHasAttender(UUID eventId, UUID userId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                select count(*) > 0
                from event_user
                where event_id = ? and user_id = ?
                """, Boolean.class, eventId, userId);
        return Boolean.TRUE.equals(exists);
    }

    private void removeEventAttender(UUID eventId, UUID userId) {
        jdbcTemplate.update("""
                delete from event_user
                where event_id = ? and user_id = ?
                """, eventId, userId);
    }

    @Nested
    @DisplayName("Get event by id tests:")
    class GetEventByIdTests {

        private UUID savedEventId;

        @BeforeEach
        void setUp() {

            savedEventId = testDataInitializer.setupFirstEvent();
            authHelper.setupSecurityContextForFirstUser();
        }

        @Test
        @DisplayName("When getting event by id should throw EventNotFoundException if event does not exist")
        public void whenGettingEventByIdShouldThrowEventNotFoundExceptionIfEventDoesNotExist() {
            assertThatThrownBy(() -> eventService.getEventById(EventConstants.NOT_EXISTING_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("When getting event by id should return event dto with correct data")
        public void whenGettingEventByIdShouldReturnDtoWithCorrectData() {
            Event testEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
            EventDto eventDto = eventService.getEventById(savedEventId);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(eventDto.getId())
                        .as("Should return correct event id")
                        .isEqualTo(savedEventId);
                softly.assertThat(eventDto.getName())
                        .as("Should return correct event name")
                        .isEqualTo(testEvent.getName());
                softly.assertThat(eventDto.getShortDescription())
                        .as("Should return correct short description")
                        .isEqualTo(testEvent.getShortDescription());
                softly.assertThat(eventDto.getLongDescription())
                        .as("Should return correct long description")
                        .isEqualTo(testEvent.getLongDescription());
                softly.assertThat(eventDto.getExactAddress())
                        .as("Should return correct exact address")
                        .isEqualTo(testEvent.getExactAddress());
                softly.assertThat(eventDto.getCity().toLowerCase())
                        .as("Should return correct city name")
                        .isEqualTo(testEvent.getCity().getName());
            });
        }
    }


    @Nested
    @DisplayName("Get user event pages tests:")
    class GetUserEventPagesTests {

        @Test
        @DisplayName("When getting user events should throw UserNotFoundException if user does not exist")
        public void whenGettingUserEventsShouldThrowUserNotFoundExceptionIfUserDoesNotExist() {
            assertThatThrownBy(() -> eventService.getUserEventsByUserId(
                    UserConstants.NOT_EXISTING_USER_ID,
                    PaginationConstants.PAGE_ZERO,
                    true))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("When getting current user attending events should throw UserNotAuthenticatedException if user is not authenticated")
        public void whenGettingCurrentUserAttendingEventsShouldThrowUserNotAuthenticatedExceptionIfUserIsNotAuthenticated() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> eventService.getCurrentUserAttendingEvents(
                    PaginationConstants.PAGE_ZERO,
                    false))
                    .isInstanceOf(UserNotAuthenticatedException.class);
        }
    }

    @Nested
    @DisplayName("Create event tests:")
    class CreateEventTests {

        private EventCreateDto eventCreateDto;

        @BeforeEach
        void setUp() {
            eventCreateDto = EventCreateDtoTestBuilder.firstEvent().build();
            authHelper.setupSecurityContextForFirstUser();
        }

        @Test
        @DisplayName("When creating event should save it with correct data and timestamps")
        public void whenCreatingEventShouldSaveItWithCorrectDataAndTimestamps() {
            UUID savedEventId = eventService.createEvent(eventCreateDto).getId();

            Event savedEvent = eventRepository.findById(savedEventId)
                    .orElseThrow(EventNotFoundException::new);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedEvent.getName())
                        .as("Should persist correct name")
                        .isEqualTo(eventCreateDto.getName());
                softly.assertThat(savedEvent.getShortDescription())
                        .as("Should persist correct short description")
                        .isEqualTo(eventCreateDto.getShortDescription());
                softly.assertThat(savedEvent.getLongDescription())
                        .as("Should persist correct long description")
                        .isEqualTo(eventCreateDto.getLongDescription());
                softly.assertThat(savedEvent.getEventStartDate())
                        .as("Should persist event start date truncated to minutes")
                        .isEqualTo(eventCreateDto.getEventStartDate().truncatedTo(ChronoUnit.MINUTES));
                softly.assertThat(savedEvent.getTimeZoneId())
                        .as("Should persist correct time zone")
                        .isEqualTo(eventCreateDto.getTimeZone());
                softly.assertThat(savedEvent.getExactAddress())
                        .as("Should persist correct exact address")
                        .isEqualTo(eventCreateDto.getExactAddress());
                softly.assertThat(savedEvent.getCreateDate())
                        .as("Create date should be set on creation")
                        .isNotNull();
                softly.assertThat(savedEvent.getLastUpdate())
                        .as("Last update should be set on creation")
                        .isNotNull();
                softly.assertThat(savedEvent.getCreateDate())
                        .as("Create date and last update should be equal on creation")
                        .isEqualTo(savedEvent.getLastUpdate());
            });
        }

        @Test
        @DisplayName("When creating event should link existing city")
        public void whenCreatingEventShouldLinkExistingCity() {
            City existingCity = cityRepository.findByIgnoreCaseName(CitiesConstants.WARSAW_NAME)
                    .orElseThrow(CityNotFoundException::new);

            UUID savedEventId = eventService.createEvent(eventCreateDto).getId();

            Event savedEvent = eventRepository.findById(savedEventId)
                    .orElseThrow(EventNotFoundException::new);

            assertThat(savedEvent.getCity().getId())
                    .as("Should link to existing city without creating a new one")
                    .isEqualTo(existingCity.getId());
        }
        @Test
        @DisplayName("When creating event should link tags to event")
        public void whenCreatingEventShouldLinkTagsToEvent() {
            UUID savedEventId = eventService.createEvent(eventCreateDto).getId();

            assertThat(findTagNamesByEventId(savedEventId))
                    .as("Persisted event should contain the same tags as the dto")
                    .isEqualTo(eventCreateDto.getTags().stream()
                            .map(String::toLowerCase)
                            .collect(Collectors.toSet()));
        }

        @Test
        @DisplayName("When creating event with empty tags should save event without tags")
        public void whenCreatingEventWithEmptyTagsShouldSaveEventWithoutTags() {
            eventCreateDto.setTags(new HashSet<>());

            UUID savedEventId = eventService.createEvent(eventCreateDto).getId();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(findTagNamesByEventId(savedEventId))
                        .as("Event should have no tags")
                        .isEmpty();
                softly.assertThat(tagRepository.count())
                        .as("No tags should be created")
                        .isZero();
            });
        }

        @Test
        @DisplayName("When creating event should normalize city and tag names to lower case")
        public void whenCreatingEventShouldNormalizeCityAndTagNamesToLowerCase() {
            eventCreateDto.setCity(CitiesConstants.WARSAW_NAME.toUpperCase());
            eventCreateDto.setTags(TagConstants.DEFAULT_EVENT_TAGS.stream()
                    .map(tag -> tag.toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet()));

            EventDto eventDto = eventService.createEvent(eventCreateDto);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(eventDto.getCity().toLowerCase())
                        .as("City name should be normalized to lower case")
                        .isEqualTo(CitiesConstants.WARSAW_NAME.toLowerCase());
                softly.assertThat(eventDto.getTags())
                        .as("Tag names should be normalized to lower case")
                        .isEqualTo(TagConstants.DEFAULT_EVENT_TAGS);
            });
        }

        @Test
        @DisplayName("When creating event should assign owner")
        public void whenCreatingEventShouldAssignOwner() {
            UUID savedEventId = eventService.createEvent(eventCreateDto).getId();

            Event savedEvent = eventRepository.findById(savedEventId)
                    .orElseThrow(EventNotFoundException::new);
            User owner = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);

            assertThat(savedEvent.getOwner().getId())
                    .as("Event owner id should match performing user")
                    .isEqualTo(owner.getId());
        }
    }


    @Nested
    @DisplayName("Update event tests:")
    class UpdateEventTests {

        private UUID savedEventId;

        @BeforeEach
        void setUp() {

            savedEventId = testDataInitializer.setupFirstEvent();
            authHelper.setupSecurityContextForFirstUser();
            eventUpdateDto = EventCreateDtoTestBuilder.updatedEvent().build();
        }

        @Test
        @DisplayName("When updating event should throw EventNotFoundException if event does not exist")
        public void whenUpdatingEventShouldThrowEventNotFoundExceptionIfEventDoesNotExist() {
            assertThatThrownBy(() -> eventService.updateEvent(eventUpdateDto, EventConstants.NOT_EXISTING_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("When updating event should throw EventAlreadyHadPlaceException if event had place")
        public void whenUpdatingEventShouldThrowEventAlreadyHadPlaceExceptionIfEventHadPlace() {
            Event eventToUpdate = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
            eventToUpdate.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
            eventRepository.save(eventToUpdate);

            assertThatThrownBy(() -> eventService.updateEvent(eventUpdateDto, savedEventId))
                    .isInstanceOf(EventAlreadyHadPlaceException.class);
        }

        @Test
        @DisplayName("When updating event should throw NotEventOwnerException if performing user does not own event")
        public void whenUpdatingEventShouldThrowNotEventOwnerExceptionIfPerformingUserDoesNotOwnEvent() {
            authHelper.setupSecurityContextForSecondUser();

            assertThatThrownBy(() -> eventService.updateEvent(eventUpdateDto, savedEventId))
                    .isInstanceOf(NotEventOwnerException.class);
        }

        @Test
        @DisplayName("When updating event should save it with correct data")
        public void whenUpdatingEventShouldSaveItWithCorrectData() {
            Instant lastUpdateBeforeUpdate = eventRepository.findById(savedEventId)
                    .orElseThrow(EventNotFoundException::new).getLastUpdate();

            eventService.updateEvent(eventUpdateDto, savedEventId);

            Event savedEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedEvent.getName())
                        .as("Should update event name")
                        .isEqualTo(EventConstants.EVENT_UPDATE_NAME);
                softly.assertThat(savedEvent.getShortDescription())
                        .as("Should update short description")
                        .isEqualTo(EventConstants.EVENT_UPDATE_SHORT_DESCRIPTION);
                softly.assertThat(savedEvent.getLongDescription())
                        .as("Should update long description")
                        .isEqualTo(EventConstants.EVENT_UPDATE_LONG_DESCRIPTION);
                softly.assertThat(savedEvent.getExactAddress())
                        .as("Should update exact address")
                        .isEqualTo(EventConstants.EVENT_UPDATE_EXACT_ADDRESS);
                softly.assertThat(savedEvent.getTimeZoneId())
                        .as("Should update time zone id")
                        .isEqualTo(eventUpdateDto.getTimeZone());
                softly.assertThat(savedEvent.getEventStartDate())
                        .as("Should update event start date truncated to minutes")
                        .isEqualTo(TimeConstants.EVENT_UPDATE_START_DATE.truncatedTo(ChronoUnit.MINUTES));
                softly.assertThat(savedEvent.getLastUpdate())
                        .as("Last update should be after the previous last update")
                        .isAfter(lastUpdateBeforeUpdate);
            });
        }

        @Test
        @DisplayName("When updating event should truncate event start date to minutes")
        public void whenUpdatingEventShouldTruncateEventStartDateToMinutes() {
            Instant dateWithSeconds = TimeConstants.EVENT_UPDATE_START_DATE.plusSeconds(30);
            eventUpdateDto.setEventStartDate(dateWithSeconds);

            eventService.updateEvent(eventUpdateDto, savedEventId);

            Event savedEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedEvent.getEventStartDate())
                        .as("Event start date should be truncated to minutes")
                        .isEqualTo(dateWithSeconds.truncatedTo(ChronoUnit.MINUTES));
                softly.assertThat(savedEvent.getEventStartDate().getNano())
                        .as("Nanos should be zeroed after truncation")
                        .isZero();
            });
        }

        @Test
        @DisplayName("When updating event should link existing city and remove event from old city")
        public void whenUpdatingEventShouldLinkExistingCityAndRemoveEventFromOldCity() {
            eventService.updateEvent(eventUpdateDto, savedEventId);

            Event savedEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
            City oldCity = cityRepository.findByIgnoreCaseName(CitiesConstants.WARSAW_NAME)
                    .orElseThrow(CityNotFoundException::new);
            City newCity = cityRepository.findByIgnoreCaseName(EventConstants.EVENT_UPDATE_CITY)
                    .orElseThrow(CityNotFoundException::new);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedEvent.getCity().getId())
                        .as("Event city should be updated to new city")
                        .isEqualTo(newCity.getId());
                softly.assertThat(savedEvent.getCity().getId())
                        .as("Event should no longer point to old city")
                        .isNotEqualTo(oldCity.getId());
            });
        }

        @Test
        @DisplayName("When updating event should replace tags on event")
        public void whenUpdatingEventShouldReplaceTagsOnEvent() {
            eventUpdateDto.setTags(TagConstants.REPLACEMENT_EVENT_TAGS);

            eventService.updateEvent(eventUpdateDto, savedEventId);

            assertThat(findTagNamesByEventId(savedEventId))
                    .as("Persisted event should contain only the updated tags")
                    .isEqualTo(TagConstants.REPLACEMENT_EVENT_TAGS);
        }


        @Test
        @DisplayName("When updating event should normalize tag names to lower case")
        public void whenUpdatingEventShouldNormalizeTagNamesToLowerCase() {
            eventUpdateDto.setTags(TagConstants.REPLACEMENT_EVENT_TAGS.stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet()));

            EventDto eventDto = eventService.updateEvent(eventUpdateDto, savedEventId);

            assertThat(eventDto.getTags())
                    .as("All tag names should be normalized to lower case")
                    .allMatch(tag -> tag.equals(tag.toLowerCase()));
        }
    }


    @Nested
    @DisplayName("Event attendance tests:")
    class EventAttendanceTests {

        private UUID savedEventId;

        @BeforeEach
        void setUp() {
            savedEventId = testDataInitializer.setupFirstEvent();
            authHelper.setupSecurityContextForSecondUser();
        }

        @Nested
        @DisplayName("Add attender to event tests:")
        class AddAttenderToEventTests {

            @Test
            @DisplayName("When adding attender to event should throw EventNotFoundException if event with given id does not exist")
            public void whenAddingAttenderToEventShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
                assertThatThrownBy(() -> eventService.addAttenderToEvent(EventConstants.NOT_EXISTING_EVENT_ID))
                        .isInstanceOf(EventNotFoundException.class);
            }

            @Test
            @DisplayName("When adding attender to event should throw EventAlreadyHadPlaceException if event start date is in the past")
            public void whenAddingAttenderToEventShouldThrowEventAlreadyHadPlaceExceptionIfEventStartDateIsInThePast() {
                Event testEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                testEvent.setEventStartDate(TimeConstants.TWO_DAYS_AGO);
                eventRepository.save(testEvent);

                assertThatThrownBy(() -> eventService.addAttenderToEvent(savedEventId))
                        .isInstanceOf(EventAlreadyHadPlaceException.class);
            }

            @Test
            @DisplayName("When adding attender to event should throw EventOwnerAlreadyAttendsEventException if event owner performs attend action")
            public void whenAddingAttenderToEventShouldThrowEventOwnerAlreadyAttendsEventExceptionIfEventOwnerPerformsAttendAction() {
                authHelper.setupSecurityContextForFirstUser();

                assertThatThrownBy(() -> eventService.addAttenderToEvent(savedEventId))
                        .isInstanceOf(EventOwnerAlreadyAttendsEventException.class);
            }

            @Test
            @DisplayName("When adding attender to event should throw AlreadyAttendingEventException if performing user is already attending event")
            public void whenAddingAttenderToEventShouldThrowAlreadyAttendingEventExceptionIfPerformingUserIsAlreadyAttendingEvent() {
                eventService.addAttenderToEvent(savedEventId);

                assertThatThrownBy(() -> eventService.addAttenderToEvent(savedEventId))
                        .isInstanceOf(AlreadyAttendingEventException.class);
            }

            @Test
            @DisplayName("When adding attender to event should persist attending relationship")
            public void whenAddingAttenderToEventShouldPersistAttendingRelationship() {
                eventService.addAttenderToEvent(savedEventId);

                User newAttender = userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL)
                        .orElseThrow(UserNotFoundException::new);

                assertThat(eventHasAttender(savedEventId, newAttender.getId()))
                        .as("Event should have the new attender in the join table")
                        .isTrue();
            }
        }

        @Nested
        @DisplayName("Remove attender from event tests:")
        class RemoveAttenderFromEventTests {

            @BeforeEach
            void setUp() {
                eventService.addAttenderToEvent(savedEventId);
            }

            @Test
            @DisplayName("When removing attender from event should throw EventNotFoundException if event with given id does not exist")
            public void whenRemovingAttenderFromEventShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
                assertThatThrownBy(() -> eventService.removeAttenderFromEvent(EventConstants.NOT_EXISTING_EVENT_ID))
                        .isInstanceOf(EventNotFoundException.class);
            }

            @Test
            @DisplayName("When removing attender from event should throw EventAlreadyHadPlaceException if event start date is in the past")
            public void whenRemovingAttenderFromEventShouldThrowEventAlreadyHadPlaceExceptionIfEventStartDateIsInThePast() {
                Event testEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                testEvent.setEventStartDate(TimeConstants.TWO_DAYS_AGO);
                eventRepository.save(testEvent);

                assertThatThrownBy(() -> eventService.removeAttenderFromEvent(savedEventId))
                        .isInstanceOf(EventAlreadyHadPlaceException.class);
            }

            @Test
            @DisplayName("When removing attender from event should throw EventOwnerMustAttendEventException if event owner performs remove action")
            public void whenRemovingAttenderFromEventShouldThrowEventOwnerMustAttendEventExceptionIfEventOwnerPerformsRemoveAction() {
                authHelper.setupSecurityContextForFirstUser();

                assertThatThrownBy(() -> eventService.removeAttenderFromEvent(savedEventId))
                        .isInstanceOf(EventOwnerMustAttendEventException.class);
            }

            @Test
            @DisplayName("When removing attender from event should throw NotEventAttenderException if performing user is not attending event")
            public void whenRemovingAttenderFromEventShouldThrowNotEventAttenderExceptionIfPerformingUserIsNotAttendingEvent() {
                User performingUser = userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL)
                        .orElseThrow(UserNotFoundException::new);
                removeEventAttender(savedEventId, performingUser.getId());

                assertThatThrownBy(() -> eventService.removeAttenderFromEvent(savedEventId))
                        .isInstanceOf(NotEventAttenderException.class);
            }

            @Test
            @DisplayName("When removing attender from event should remove attending relationship")
            public void whenRemovingAttenderFromEventShouldRemoveAttendingRelationship() {
                eventService.removeAttenderFromEvent(savedEventId);

                User performingUser = userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL)
                        .orElseThrow(UserNotFoundException::new);

                assertThat(eventHasAttender(savedEventId, performingUser.getId()))
                        .as("Event should no longer have the removed attender in the join table")
                        .isFalse();
            }
        }
    }

}
