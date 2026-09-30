package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventAttendeePageDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.city.CityNotFoundException;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.*;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.notification.service.NotificationCommandService;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

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
    @MockitoSpyBean
    private NotificationCommandService notificationCommandService;

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

    private boolean eventHasAttendee(UUID eventId, UUID userId) {
        Boolean exists = jdbcTemplate.queryForObject("""
                select count(*) > 0
                from event_user
                where event_id = ? and user_id = ?
                """, Boolean.class, eventId, userId);
        return Boolean.TRUE.equals(exists);
    }

    private void removeEventAttendee(UUID eventId, UUID userId) {
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
                softly.assertThat(eventDto.getCity())
                        .as("Should return correct city name")
                        .isEqualTo(testEvent.getCity().getName());
            });
        }

        @Test
        @DisplayName("When getting event by id should count attendees without counting the owner")
        void whenGettingEventByIdShouldCountOnlyAttendees() {
            testDataInitializer.addSecondUserToAttendees(savedEventId);

            EventDto eventDto = eventService.getEventById(savedEventId);

            assertThat(eventDto.getAttendeeCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Get event attendees tests:")
    class GetEventAttendeesTests {

        private UUID savedEventId;

        @BeforeEach
        void setUp() {
            savedEventId = testDataInitializer.setupFirstEvent();
        }

        @Test
        @DisplayName("Owner can see an empty attendee page")
        void ownerCanSeeEmptyAttendeePage() {
            authHelper.setupSecurityContextForFirstUser();

            EventAttendeePageDto page = eventService.getEventAttendees(savedEventId, 0);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(page.attendees()).isEmpty();
                softly.assertThat(page.pageNumber()).isZero();
                softly.assertThat(page.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(page.totalElements()).isZero();
                softly.assertThat(page.totalPages()).isZero();
                softly.assertThat(page.lastPage()).isTrue();
            });
        }

        @Test
        @DisplayName("Attendee can see attendee profiles while owner is excluded")
        void attendeeCanSeeProfilesWithoutOwner() {
            testDataInitializer.addSecondUserToAttendees(savedEventId);
            authHelper.setupSecurityContextForSecondUser();
            User attendee = userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL).orElseThrow();
            User owner = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow();

            EventAttendeePageDto page = eventService.getEventAttendees(savedEventId, 0);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(page.attendees()).hasSize(1);
                softly.assertThat(page.attendees().getFirst().getId()).isEqualTo(attendee.getId());
                softly.assertThat(page.attendees().getFirst().getFirstName()).isEqualTo(attendee.getFirstName());
                softly.assertThat(page.attendees().getFirst().getHomeCity())
                        .isEqualTo(attendee.getHomeCity().getName());
                softly.assertThat(page.attendees().stream().map(profile -> profile.getId()))
                        .doesNotContain(owner.getId());
                softly.assertThat(page.totalElements()).isEqualTo(1);
            });
        }

        @Test
        @DisplayName("Attendee pages are ordered and contain all attendees exactly once")
        void attendeePagesHaveStableOrderAndCorrectTotals() {
            testDataInitializer.addSecondUserToAttendees(savedEventId);
            User existingAttendee = userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL).orElseThrow();
            User owner = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow();

            for (int index = 0; index < PaginationConstants.DEFAULT_PAGE_SIZE; index++) {
                User attendee = userRepository.save(UserTestBuilder.firstUser()
                        .id(null)
                        .firstName("Attendee %02d".formatted(index))
                        .lastName("Member")
                        .email("event.attendee.%02d@example.com".formatted(index))
                        .homeCity(owner.getHomeCity())
                        .roles(owner.getRoles())
                        .build());
                jdbcTemplate.update("insert into event_user (event_id, user_id) values (?, ?)",
                        savedEventId, attendee.getId());
            }
            jdbcTemplate.update("update events set attendee_count = ? where id = ?",
                    PaginationConstants.DEFAULT_PAGE_SIZE + 1, savedEventId);
            authHelper.setupSecurityContextForFirstUser();

            EventAttendeePageDto firstPage = eventService.getEventAttendees(savedEventId, 0);
            EventAttendeePageDto secondPage = eventService.getEventAttendees(savedEventId, 1);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(firstPage.attendees()).hasSize(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(firstPage.attendees().stream().map(profile -> profile.getFirstName()))
                        .containsExactlyElementsOf(IntStream.range(0, PaginationConstants.DEFAULT_PAGE_SIZE)
                                .mapToObj(index -> "Attendee %02d".formatted(index))
                                .toList());
                softly.assertThat(firstPage.pageNumber()).isZero();
                softly.assertThat(firstPage.totalElements()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE + 1);
                softly.assertThat(firstPage.totalPages()).isEqualTo(2);
                softly.assertThat(firstPage.lastPage()).isFalse();
                softly.assertThat(secondPage.attendees()).hasSize(1);
                softly.assertThat(secondPage.attendees().getFirst().getId()).isEqualTo(existingAttendee.getId());
                softly.assertThat(secondPage.pageNumber()).isEqualTo(1);
                softly.assertThat(secondPage.totalElements()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE + 1);
                softly.assertThat(secondPage.totalPages()).isEqualTo(2);
                softly.assertThat(secondPage.lastPage()).isTrue();
                softly.assertThat(firstPage.attendees().stream().map(profile -> profile.getId()))
                        .doesNotContain(owner.getId(), existingAttendee.getId());
                softly.assertThat(firstPage.attendees().stream().map(profile -> profile.getId()).toList())
                        .doesNotHaveDuplicates();
            });
        }

        @Test
        @DisplayName("Unauthenticated caller cannot read attendees")
        void unauthenticatedCallerCannotReadAttendees() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> eventService.getEventAttendees(savedEventId, 0))
                    .isInstanceOf(UserNotAuthenticatedException.class);
        }

        @Test
        @DisplayName("Outsider cannot read attendee profiles")
        void outsiderCannotReadAttendees() {
            authHelper.setupSecurityContextForSecondUser();

            assertThatThrownBy(() -> eventService.getEventAttendees(savedEventId, 0))
                    .isInstanceOf(NotEventAttendeeException.class);
        }

        @Test
        @DisplayName("Missing event is rejected")
        void missingEventIsRejected() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> eventService.getEventAttendees(EventConstants.NOT_EXISTING_EVENT_ID, 0))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("Negative attendee page is rejected")
        void negativePageIsRejected() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> eventService.getEventAttendees(savedEventId, -1))
                    .isInstanceOf(InvalidPageNumberException.class);
        }
    }


    @Nested
    @DisplayName("Get events tests:")
    class GetEventsTests {

        @Test
        @DisplayName("When getting events should return event overview page with persisted events")
        public void whenGettingEventsShouldReturnEventOverviewPageWithPersistedEvents() {
            UUID firstEventId = testDataInitializer.setupFirstEvent();
            UUID secondEventId = testDataInitializer.setupEventBySecondUser();

            EventOverviewPageDto result = eventService.getEvents(PaginationConstants.PAGE_ZERO);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getEvents())
                        .as("Should return persisted events")
                        .hasSize(2);
                softly.assertThat(result.getEvents().stream().map(event -> event.getId()).toList())
                        .as("Should return correct event ids")
                        .containsExactlyInAnyOrder(firstEventId, secondEventId);
                softly.assertThat(result.getEvents())
                        .as("Should include the persisted event timezone in every overview")
                        .allSatisfy(event -> softly.assertThat(event.getTimeZone())
                                .isEqualTo(UserConstants.FIRST_USER_TIMEZONE));
                softly.assertThat(result.getPageNumber())
                        .as("Should return requested page number")
                        .isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(result.getPageSize())
                        .as("Should return default event page size")
                        .isEqualTo(PaginationConstants.EVENT_PAGE_SIZE);
                softly.assertThat(result.getTotalElements())
                        .as("Should return total event count")
                        .isEqualTo(2);
                softly.assertThat(result.isLastPage())
                        .as("Should mark page as last when all events fit on first page")
                        .isTrue();
            });
        }

        @Test
        @DisplayName("When getting events should return empty page if no events exist")
        public void whenGettingEventsShouldReturnEmptyPageIfNoEventsExist() {
            EventOverviewPageDto result = eventService.getEvents(PaginationConstants.PAGE_ZERO);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getEvents())
                        .as("Should return no events")
                        .isEmpty();
                softly.assertThat(result.getPageNumber())
                        .as("Should return requested page number")
                        .isEqualTo(PaginationConstants.PAGE_ZERO);
                softly.assertThat(result.getPageSize())
                        .as("Should return default event page size")
                        .isEqualTo(PaginationConstants.EVENT_PAGE_SIZE);
                softly.assertThat(result.getTotalElements())
                        .as("Should return zero total elements")
                        .isZero();
                softly.assertThat(result.getTotalPages())
                        .as("Should return zero total pages")
                        .isZero();
                softly.assertThat(result.isLastPage())
                        .as("Empty page should be marked as last")
                        .isTrue();
            });
        }

        @Test
        @DisplayName("When getting events should throw InvalidPageNumberException if page is negative")
        public void whenGettingEventsShouldThrowInvalidPageNumberExceptionIfPageIsNegative() {
            assertThatThrownBy(() -> eventService.getEvents(PaginationConstants.PAGE_MINUS_ONE))
                    .isInstanceOf(InvalidPageNumberException.class);
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
        @DisplayName("When getting user events should throw InvalidPageNumberException if page is negative")
        public void whenGettingUserEventsShouldThrowInvalidPageNumberExceptionIfPageIsNegative() {
            assertThatThrownBy(() -> eventService.getUserEventsByUserId(
                    UserConstants.FIRST_USER_ID,
                    PaginationConstants.PAGE_MINUS_ONE,
                    true))
                    .isInstanceOf(InvalidPageNumberException.class);
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

        @Test
        @DisplayName("When getting current user attending events should throw InvalidPageNumberException if page is negative")
        public void whenGettingCurrentUserAttendingEventsShouldThrowInvalidPageNumberExceptionIfPageIsNegative() {
            authHelper.setupSecurityContextForFirstUser();

            assertThatThrownBy(() -> eventService.getCurrentUserAttendingEvents(
                    PaginationConstants.PAGE_MINUS_ONE,
                    false))
                    .isInstanceOf(InvalidPageNumberException.class);
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
                softly.assertThat(eventDto.getCity())
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
                        .as("Last update should use the fixed application clock")
                        .isEqualTo(TimeConstants.NOW);
            });
        }

        @Test
        @DisplayName("When event update notification fails should roll back event changes")
        public void whenEventUpdateNotificationFailsShouldRollBackEventChanges() {
            String originalEventName = eventRepository.findById(savedEventId)
                    .orElseThrow(EventNotFoundException::new)
                    .getName();
            RuntimeException notificationFailure = new RuntimeException("Notification creation failed");

            doThrow(notificationFailure)
                    .when(notificationCommandService)
                    .notifyEventUpdated(eq(savedEventId), anyCollection(), anyString());

            assertThatThrownBy(() -> eventService.updateEvent(eventUpdateDto, savedEventId))
                    .isSameAs(notificationFailure);

            Event storedEvent = eventRepository.findById(savedEventId)
                    .orElseThrow(EventNotFoundException::new);

            assertThat(storedEvent.getName())
                    .as("Event changes should roll back when notification creation fails")
                    .isEqualTo(originalEventName);
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
        @DisplayName("Add attendee to event tests:")
        class AddAttendeeToEventTests {

            @Test
            @DisplayName("When adding attendee to event should throw EventNotFoundException if event with given id does not exist")
            public void whenAddingAttendeeToEventShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
                assertThatThrownBy(() -> eventService.addAttendeeToEvent(EventConstants.NOT_EXISTING_EVENT_ID))
                        .isInstanceOf(EventNotFoundException.class);
            }

            @Test
            @DisplayName("When adding attendee to event should throw EventAlreadyHadPlaceException if event start date is in the past")
            public void whenAddingAttendeeToEventShouldThrowEventAlreadyHadPlaceExceptionIfEventStartDateIsInThePast() {
                Event testEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                testEvent.setEventStartDate(TimeConstants.TWO_DAYS_AGO);
                eventRepository.save(testEvent);

                assertThatThrownBy(() -> eventService.addAttendeeToEvent(savedEventId))
                        .isInstanceOf(EventAlreadyHadPlaceException.class);
            }

            @Test
            @DisplayName("When adding attendee to event should throw EventOwnerAlreadyAttendsEventException if event owner performs attend action")
            public void whenAddingAttendeeToEventShouldThrowEventOwnerAlreadyAttendsEventExceptionIfEventOwnerPerformsAttendAction() {
                authHelper.setupSecurityContextForFirstUser();

                assertThatThrownBy(() -> eventService.addAttendeeToEvent(savedEventId))
                        .isInstanceOf(EventOwnerAlreadyAttendsEventException.class);
            }

            @Test
            @DisplayName("When adding attendee to event should throw AlreadyAttendingEventException if performing user is already attending event")
            public void whenAddingAttendeeToEventShouldThrowAlreadyAttendingEventExceptionIfPerformingUserIsAlreadyAttendingEvent() {
                eventService.addAttendeeToEvent(savedEventId);

                assertThatThrownBy(() -> eventService.addAttendeeToEvent(savedEventId))
                        .isInstanceOf(AlreadyAttendingEventException.class);
            }

            @Test
            @DisplayName("When adding attendee to event should persist attending relationship")
            public void whenAddingAttendeeToEventShouldPersistAttendingRelationship() {
                eventService.addAttendeeToEvent(savedEventId);

                User newAttendee = userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL)
                        .orElseThrow(UserNotFoundException::new);

                assertThat(eventHasAttendee(savedEventId, newAttendee.getId()))
                        .as("Event should have the new attendee in the join table")
                        .isTrue();
            }
        }

        @Nested
        @DisplayName("Remove attendee from event tests:")
        class RemoveAttendeeFromEventTests {

            @BeforeEach
            void setUp() {
                eventService.addAttendeeToEvent(savedEventId);
            }

            @Test
            @DisplayName("When removing attendee from event should throw EventNotFoundException if event with given id does not exist")
            public void whenRemovingAttendeeFromEventShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
                assertThatThrownBy(() -> eventService.removeAttendeeFromEvent(EventConstants.NOT_EXISTING_EVENT_ID))
                        .isInstanceOf(EventNotFoundException.class);
            }

            @Test
            @DisplayName("When removing attendee from event should throw EventAlreadyHadPlaceException if event start date is in the past")
            public void whenRemovingAttendeeFromEventShouldThrowEventAlreadyHadPlaceExceptionIfEventStartDateIsInThePast() {
                Event testEvent = eventRepository.findById(savedEventId).orElseThrow(EventNotFoundException::new);
                testEvent.setEventStartDate(TimeConstants.TWO_DAYS_AGO);
                eventRepository.save(testEvent);

                assertThatThrownBy(() -> eventService.removeAttendeeFromEvent(savedEventId))
                        .isInstanceOf(EventAlreadyHadPlaceException.class);
            }

            @Test
            @DisplayName("When removing attendee from event should throw EventOwnerMustAttendEventException if event owner performs remove action")
            public void whenRemovingAttendeeFromEventShouldThrowEventOwnerMustAttendEventExceptionIfEventOwnerPerformsRemoveAction() {
                authHelper.setupSecurityContextForFirstUser();

                assertThatThrownBy(() -> eventService.removeAttendeeFromEvent(savedEventId))
                        .isInstanceOf(EventOwnerMustAttendEventException.class);
            }

            @Test
            @DisplayName("When removing attendee from event should throw NotEventAttendeeException if performing user is not attending event")
            public void whenRemovingAttendeeFromEventShouldThrowNotEventAttendeeExceptionIfPerformingUserIsNotAttendingEvent() {
                User performingUser = userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL)
                        .orElseThrow(UserNotFoundException::new);
                removeEventAttendee(savedEventId, performingUser.getId());

                assertThatThrownBy(() -> eventService.removeAttendeeFromEvent(savedEventId))
                        .isInstanceOf(NotEventAttendeeException.class);
            }

            @Test
            @DisplayName("When removing attendee from event should remove attending relationship")
            public void whenRemovingAttendeeFromEventShouldRemoveAttendingRelationship() {
                eventService.removeAttendeeFromEvent(savedEventId);

                User performingUser = userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL)
                        .orElseThrow(UserNotFoundException::new);

                assertThat(eventHasAttendee(savedEventId, performingUser.getId()))
                        .as("Event should no longer have the removed attendee in the join table")
                        .isFalse();
            }
        }
    }

}
