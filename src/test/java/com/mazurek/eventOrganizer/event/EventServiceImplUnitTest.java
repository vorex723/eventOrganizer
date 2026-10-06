package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityServiceImpl;
import com.mazurek.eventOrganizer.config.properties.PaginationProperties;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventAttendeePageDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.*;
import com.mazurek.eventOrganizer.notification.service.NotificationCommandService;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.tag.TagService;
import com.mazurek.eventOrganizer.testData.builders.*;
import com.mazurek.eventOrganizer.testData.builders.dto.EventCreateDtoTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventServiceImpl unit tests:")
class EventServiceImplUnitTest {

    private EventServiceImpl eventService;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private TagService tagService;
    @Mock
    private CityServiceImpl cityService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationCommandService notificationCommandService;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private PaginationProperties paginationProperties;
    private final Clock clock = TimeConstants.FIXED_CLOCK;

    private User firstUser;
    private User secondUser;
    private Event event;

    private Tag tagOne;
    private Tag tagTwo;
    private Tag tagThree;

    private City cityKrakow;
    private City cityWarsaw;

    private EventCreateDto eventCreateDto;
    private EventCreateDto updatedEventDto;

    private Optional<Event> eventOptional;

    @BeforeEach
    void setUp() {
        eventService = new EventServiceImpl(
                eventRepository,
                userRepository,
                notificationCommandService,
                authenticationService,
                cityService,
                tagService,
                paginationProperties,
                clock
        );
        cityWarsaw = CityTestBuilder.warsaw().build();

        firstUser = UserTestBuilder.firstUser().homeCity(cityWarsaw).build();
        secondUser = UserTestBuilder.secondUser().homeCity(cityWarsaw).build();

        tagOne = TagTestBuilder.firstTag().build();
        tagTwo = TagTestBuilder.secondTag().build();
        tagThree = TagTestBuilder.thirdTag().build();

        event = EventTestBuilder.firstEvent().owner(firstUser).city(cityWarsaw).build();
        eventOptional = Optional.of(event);

        event.addTag(tagOne);
        event.addTag(tagTwo);

        eventCreateDto = EventCreateDtoTestBuilder.firstEvent().build();
    }

    @Nested
    @DisplayName("Get event by id tests:")
    class GetEventByIdTests {

        @Test
        @DisplayName("When getting event by id should load event from database")
        public void whenGettingEventByIdShouldLoadEventFromDatabase() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);

            eventService.getEventById(EventConstants.FIRST_EVENT_ID);

            verify(eventRepository, times(1)).findById(EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When getting event by id should throw EventNotFoundException if event does not exist")
        public void whenGettingEventByIdShouldThrowEventNotFoundExceptionIfEventDoesNotExist() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.getEventById(EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("When getting event by id should return event dto with correct data")
        public void whenGettingEventByIdShouldReturnDtoWithCorrectData() {
            event.addAttendee(secondUser);
            firstUser.setTimeZone("Asia/Tokyo");
            assertThat(firstUser.getTimeZone()).isNotEqualTo(event.getCity().getTimeZoneId());
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);

            EventDto output = eventService.getEventById(EventConstants.FIRST_EVENT_ID);

            assertThat(output).isNotNull();
            assertThat(output.getOwner()).isNotNull();
            assertThat(output).extracting(EventDto::getCityId, EventDto::getCityExternalId,
                            EventDto::getCity, EventDto::getTimeZone, EventDto::getExactAddress,
                            EventDto::getEventStartDate, EventDto::getMaxAttendees,
                            EventDto::getCreateDate, EventDto::getLastUpdate)
                    .containsExactly(cityWarsaw.getId(), cityWarsaw.getExternalId(), cityWarsaw.getName(),
                            cityWarsaw.getTimeZoneId(), EventConstants.FIRST_EVENT_ADDRESS,
                            TimeConstants.ONE_WEEK_FROM_NOW, EventConstants.DEFAULT_MAX_ATTENDEES,
                            TimeConstants.NOW, TimeConstants.NOW);
            assertThat(output.getOwner()).extracting(profile -> profile.getId(),
                            profile -> profile.getFirstName(), profile -> profile.getLastName())
                    .containsExactly(firstUser.getId(), firstUser.getFirstName(), firstUser.getLastName());
            assertThat(output.getTags()).containsExactlyInAnyOrder(tagOne.getName(), tagTwo.getName());
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.getId())
                        .as("Should return correct event id")
                        .isEqualTo(event.getId());
                softly.assertThat(output.getName())
                        .as("Should return correct event name")
                        .isEqualTo(event.getName());
                softly.assertThat(output.getShortDescription())
                        .as("Should return correct short description")
                        .isEqualTo(event.getShortDescription());
                softly.assertThat(output.getLongDescription())
                        .as("Should return correct long description")
                        .isEqualTo(event.getLongDescription());
                softly.assertThat(output.getAttendeeCount())
                        .as("Should return correct attendee count")
                        .isEqualTo(event.getAttendeeCount());
            });
        }
    }

    @Nested
    @DisplayName("Get event attendees tests:")
    class GetEventAttendeesTests {

        @Test
        @DisplayName("When getting attendees should return a mapped attendee page")
        void whenGettingAttendeesShouldReturnMappedAttendeePage() {
            when(paginationProperties.getDefaultPageSize()).thenReturn(PaginationConstants.DEFAULT_PAGE_SIZE);
            Page<User> attendeePage = new PageImpl<>(
                    List.of(secondUser),
                    PageRequest.of(PaginationConstants.PAGE_ZERO, PaginationConstants.DEFAULT_PAGE_SIZE),
                    1
            );
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(authenticationService.getCurrentUserId()).thenReturn(UserConstants.FIRST_USER_ID);
            when(eventRepository.isUserAttendeeOrOwner(UserConstants.FIRST_USER_ID, EventConstants.FIRST_EVENT_ID))
                    .thenReturn(true);
            when(eventRepository.findAttendeesByEventId(eq(EventConstants.FIRST_EVENT_ID), any(Pageable.class)))
                    .thenReturn(attendeePage);

            EventAttendeePageDto result = eventService.getEventAttendees(
                    EventConstants.FIRST_EVENT_ID,
                    PaginationConstants.PAGE_ZERO
            );

            verify(eventRepository).existsById(EventConstants.FIRST_EVENT_ID);
            verify(eventRepository, never()).findById(any(UUID.class));

            assertThat(result).isNotNull();
            assertThat(result.attendees()).singleElement().satisfies(profile -> {
                assertThat(profile.getId()).isEqualTo(secondUser.getId());
                assertThat(profile.getFirstName()).isEqualTo(secondUser.getFirstName());
                assertThat(profile.getLastName()).isEqualTo(secondUser.getLastName());
            });
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.attendees()).hasSize(1);
                softly.assertThat(result.pageNumber()).isZero();
                softly.assertThat(result.pageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(result.totalElements()).isEqualTo(1);
                softly.assertThat(result.totalPages()).isEqualTo(1);
                softly.assertThat(result.lastPage()).isTrue();
            });
        }

        @Test
        @DisplayName("When getting attendees should throw EventNotFoundException if event does not exist")
        void whenGettingAttendeesShouldThrowIfEventDoesNotExist() {
            when(eventRepository.existsById(EventConstants.NOT_EXISTING_EVENT_ID)).thenReturn(false);

            assertThatThrownBy(() -> eventService.getEventAttendees(
                    EventConstants.NOT_EXISTING_EVENT_ID,
                    PaginationConstants.PAGE_ZERO
            )).isInstanceOf(EventNotFoundException.class);

            verify(authenticationService, never()).getCurrentUserId();
            verify(eventRepository, never()).findAttendeesByEventId(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("When getting attendees should reject a user who is neither owner nor attendee")
        void whenGettingAttendeesShouldRejectOutsider() {
            when(eventRepository.existsById(EventConstants.FIRST_EVENT_ID)).thenReturn(true);
            when(authenticationService.getCurrentUserId()).thenReturn(UserConstants.SECOND_USER_ID);
            when(eventRepository.isUserAttendeeOrOwner(UserConstants.SECOND_USER_ID, EventConstants.FIRST_EVENT_ID))
                    .thenReturn(false);

            assertThatThrownBy(() -> eventService.getEventAttendees(
                    EventConstants.FIRST_EVENT_ID,
                    PaginationConstants.PAGE_ZERO
            )).isInstanceOf(NotEventAttendeeException.class);

            verify(eventRepository, never()).findAttendeesByEventId(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("When getting attendees should reject a negative page number before querying")
        void whenGettingAttendeesShouldRejectNegativePageNumber() {
            assertThatThrownBy(() -> eventService.getEventAttendees(
                    EventConstants.FIRST_EVENT_ID,
                    PaginationConstants.PAGE_MINUS_ONE
            )).isInstanceOf(InvalidPageNumberException.class);

            verify(eventRepository, never()).existsById(any(UUID.class));
        }
    }

    @Nested
    @DisplayName("Get events tests:")
    class GetEventsTests {

        @Test
        @DisplayName("When getting events should use page number and default page size in repository query")
        public void whenGettingEventsShouldUsePageNumberAndDefaultPageSizeInRepositoryQuery() {
            when(paginationProperties.getDefaultPageSize()).thenReturn(PaginationConstants.DEFAULT_PAGE_SIZE);
            int pageNumber = 1;
            Page<Event> eventPage = new PageImpl<>(
                    List.of(event),
                    PageRequest.of(pageNumber, PaginationConstants.DEFAULT_PAGE_SIZE),
                    21
            );
            when(eventRepository.findAll(any(Pageable.class))).thenReturn(eventPage);

            eventService.getEvents(pageNumber);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(eventRepository, times(1)).findAll(pageableCaptor.capture());
            Pageable capturedPageable = pageableCaptor.getValue();

            assertThat(capturedPageable).isNotNull();
            assertThat(capturedPageable.getSort()).containsExactly(
                    Sort.Order.desc("eventStartDate"), Sort.Order.desc("id"));
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedPageable.getPageNumber()).isEqualTo(pageNumber);
                softly.assertThat(capturedPageable.getPageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(capturedPageable.getSort().getOrderFor("eventStartDate")).isNotNull();
                softly.assertThat(capturedPageable.getSort().getOrderFor("id")).isNotNull();
            });
        }

        @Test
        @DisplayName("When getting events should return empty page if requested page is empty")
        public void whenGettingEventsShouldReturnEmptyPageIfRequestedPageIsEmpty() {
            when(paginationProperties.getDefaultPageSize()).thenReturn(PaginationConstants.DEFAULT_PAGE_SIZE);
            when(eventRepository.findAll(any(Pageable.class)))
                    .thenReturn(Page.empty(PageRequest.of(PaginationConstants.PAGE_ZERO, PaginationConstants.DEFAULT_PAGE_SIZE)));

            EventOverviewPageDto result = eventService.getEvents(PaginationConstants.PAGE_ZERO);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getEvents()).isEmpty();
                softly.assertThat(result.getPageNumber()).isZero();
                softly.assertThat(result.getPageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(result.getTotalElements()).isZero();
                softly.assertThat(result.getTotalPages()).isZero();
                softly.assertThat(result.isLastPage()).isTrue();
            });
        }

        @Test
        @DisplayName("When getting events should throw InvalidPageNumberException if page number is below zero")
        public void whenGettingEventsShouldThrowInvalidPageNumberExceptionIfPageNumberIsBelowZero() {
            assertThatThrownBy(() -> eventService.getEvents(PaginationConstants.PAGE_MINUS_ONE))
                    .isInstanceOf(InvalidPageNumberException.class);

            verify(eventRepository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Get user event pages tests:")
    class UserEventPagesTests {

        @Test
        @DisplayName("When getting user events with upcoming flag should use upcoming query")
        public void whenGettingUserEventsWithUpcomingFlagShouldUseUpcomingQuery() {
            when(paginationProperties.getDefaultPageSize()).thenReturn(PaginationConstants.DEFAULT_PAGE_SIZE);
            Page<Event> eventPage = new PageImpl<>(
                    List.of(event),
                    PageRequest.of(PaginationConstants.PAGE_ZERO, PaginationConstants.DEFAULT_PAGE_SIZE),
                    1
            );
            when(userRepository.findById(UserConstants.FIRST_USER_ID)).thenReturn(Optional.of(firstUser));
            when(eventRepository.findUpcomingEventsByOwnerId(eq(UserConstants.FIRST_USER_ID), eq(TimeConstants.NOW), any(Pageable.class)))
                    .thenReturn(eventPage);

            EventOverviewPageDto result = eventService.getUserEventsByUserId(UserConstants.FIRST_USER_ID, 0, true);

            assertThat(result.getEvents()).hasSize(1);
            assertThat(result.getEvents().getFirst().getTimeZone()).isEqualTo(event.getCity().getTimeZoneId());
            verify(userRepository, times(1)).findById(UserConstants.FIRST_USER_ID);
            verify(eventRepository, times(1))
                    .findUpcomingEventsByOwnerId(eq(UserConstants.FIRST_USER_ID), eq(TimeConstants.NOW), any(Pageable.class));
            verify(eventRepository, never())
                    .findByOwnerId(eq(UserConstants.FIRST_USER_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("When getting user events without upcoming flag should use all-events query")
        public void whenGettingUserEventsWithoutUpcomingFlagShouldUseAllEventsQuery() {
            when(paginationProperties.getDefaultPageSize()).thenReturn(PaginationConstants.DEFAULT_PAGE_SIZE);
            Page<Event> eventPage = new PageImpl<>(
                    List.of(event),
                    PageRequest.of(PaginationConstants.PAGE_ZERO, PaginationConstants.DEFAULT_PAGE_SIZE),
                    1
            );
            when(userRepository.findById(UserConstants.FIRST_USER_ID)).thenReturn(Optional.of(firstUser));
            when(eventRepository.findByOwnerId(eq(UserConstants.FIRST_USER_ID), any(Pageable.class)))
                    .thenReturn(eventPage);

            EventOverviewPageDto result = eventService.getUserEventsByUserId(UserConstants.FIRST_USER_ID, 0, false);

            assertThat(result.getEvents()).hasSize(1);
            verify(userRepository, times(1)).findById(UserConstants.FIRST_USER_ID);
            verify(eventRepository, times(1))
                    .findByOwnerId(eq(UserConstants.FIRST_USER_ID), any(Pageable.class));
            verify(eventRepository, never())
                    .findUpcomingEventsByOwnerId(eq(UserConstants.FIRST_USER_ID), any(Instant.class), any(Pageable.class));
        }

        @Test
        @DisplayName("When getting current user attending events with upcoming flag should use upcoming-attending query")
        public void whenGettingCurrentUserAttendingEventsWithUpcomingFlagShouldUseUpcomingAttendingQuery() {
            when(paginationProperties.getDefaultPageSize()).thenReturn(PaginationConstants.DEFAULT_PAGE_SIZE);
            Page<Event> eventPage = new PageImpl<>(
                    List.of(event),
                    PageRequest.of(PaginationConstants.PAGE_ZERO, PaginationConstants.DEFAULT_PAGE_SIZE),
                    1
            );
            when(authenticationService.getCurrentUserId()).thenReturn(UserConstants.FIRST_USER_ID);
            when(eventRepository.findUpcomingUserAttendingEventsByUserId(eq(UserConstants.FIRST_USER_ID), eq(TimeConstants.NOW), any(Pageable.class)))
                    .thenReturn(eventPage);

            EventOverviewPageDto result = eventService.getCurrentUserAttendingEvents(0, true);

            assertThat(result.getEvents()).hasSize(1);
            verify(authenticationService, times(1)).getCurrentUserId();
            verify(eventRepository, times(1))
                    .findUpcomingUserAttendingEventsByUserId(eq(UserConstants.FIRST_USER_ID), eq(TimeConstants.NOW), any(Pageable.class));
            verify(eventRepository, never())
                    .findUserAttendingEventsByUserId(eq(UserConstants.FIRST_USER_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("When getting current user attending events without upcoming flag should use all-attending query")
        public void whenGettingCurrentUserAttendingEventsWithoutUpcomingFlagShouldUseAllAttendingQuery() {
            when(paginationProperties.getDefaultPageSize()).thenReturn(PaginationConstants.DEFAULT_PAGE_SIZE);
            Page<Event> eventPage = new PageImpl<>(
                    List.of(event),
                    PageRequest.of(PaginationConstants.PAGE_ZERO, PaginationConstants.DEFAULT_PAGE_SIZE),
                    1
            );
            when(authenticationService.getCurrentUserId()).thenReturn(UserConstants.FIRST_USER_ID);
            when(eventRepository.findUserAttendingEventsByUserId(eq(UserConstants.FIRST_USER_ID), any(Pageable.class)))
                    .thenReturn(eventPage);

            EventOverviewPageDto result = eventService.getCurrentUserAttendingEvents(0, false);

            assertThat(result.getEvents()).hasSize(1);
            verify(authenticationService, times(1)).getCurrentUserId();
            verify(eventRepository, times(1))
                    .findUserAttendingEventsByUserId(eq(UserConstants.FIRST_USER_ID), any(Pageable.class));
            verify(eventRepository, never())
                    .findUpcomingUserAttendingEventsByUserId(eq(UserConstants.FIRST_USER_ID), any(Instant.class), any(Pageable.class));
        }

        @Test
        @DisplayName("When getting user events should throw UserNotFoundException if user does not exist")
        public void whenGettingUserEventsShouldThrowUserNotFoundExceptionIfUserDoesNotExist() {
            when(userRepository.findById(UserConstants.NOT_EXISTING_USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.getUserEventsByUserId(UserConstants.NOT_EXISTING_USER_ID, 0, true))
                    .isInstanceOf(UserNotFoundException.class);

            verify(eventRepository, never()).findUpcomingEventsByOwnerId(any(UUID.class), any(Instant.class), any(Pageable.class));
            verify(eventRepository, never()).findByOwnerId(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("When getting current user attending events should propagate UserNotAuthenticatedException if user is not authenticated")
        public void whenGettingCurrentUserAttendingEventsShouldPropagateUserNotAuthenticatedExceptionIfUserIsNotAuthenticated() {
            when(authenticationService.getCurrentUserId()).thenThrow(new UserNotAuthenticatedException());

            assertThatThrownBy(() -> eventService.getCurrentUserAttendingEvents(0, true))
                    .isInstanceOf(UserNotAuthenticatedException.class);

            verify(eventRepository, never()).findUpcomingUserAttendingEventsByUserId(any(UUID.class), any(Instant.class), any(Pageable.class));
            verify(eventRepository, never()).findUserAttendingEventsByUserId(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("When getting user events should throw InvalidPageNumberException if page number is below zero")
        public void whenGettingUserEventsShouldThrowInvalidPageNumberExceptionIfPageNumberIsBelowZero() {
            assertThatThrownBy(() -> eventService.getUserEventsByUserId(UserConstants.FIRST_USER_ID, PaginationConstants.PAGE_MINUS_ONE, true))
                    .isInstanceOf(InvalidPageNumberException.class);

            verify(userRepository, never()).findById(any(UUID.class));
            verify(eventRepository, never()).findUpcomingEventsByOwnerId(any(UUID.class), any(Instant.class), any(Pageable.class));
            verify(eventRepository, never()).findByOwnerId(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("When getting current user attending events should throw InvalidPageNumberException if page number is below zero")
        public void whenGettingCurrentUserAttendingEventsShouldThrowInvalidPageNumberExceptionIfPageNumberIsBelowZero() {
            assertThatThrownBy(() -> eventService.getCurrentUserAttendingEvents(PaginationConstants.PAGE_MINUS_ONE, true))
                    .isInstanceOf(InvalidPageNumberException.class);

            verify(authenticationService, never()).getCurrentUserId();
            verify(eventRepository, never()).findUpcomingUserAttendingEventsByUserId(any(UUID.class), any(Instant.class), any(Pageable.class));
            verify(eventRepository, never()).findUserAttendingEventsByUserId(any(UUID.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("Create event tests:")
    class CreateEventTests {
        private Set<Tag> defaultTags;

        @BeforeEach
        void setUp() {
            defaultTags = new HashSet<>(Set.of(tagOne, tagTwo));
        }

        private void setupSuccessfulEventCreateMocks() {
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(cityService.resolve(com.mazurek.eventOrganizer.testData.TestCityData.externalId(CitiesConstants.WARSAW_NAME))).thenReturn(cityWarsaw);
            when(tagService.getTagsByNames(eventCreateDto.getTags())).thenReturn(defaultTags);
            when(eventRepository.save(any())).thenReturn(event);
        }

        @Test
        @DisplayName("When creating event should retrieve performing user using AuthenticationService")
        public void whenCreatingEventShouldRetrievePerformingUserUsingAuthenticationService() {
            setupSuccessfulEventCreateMocks();

            eventService.createEvent(eventCreateDto);

            verify(authenticationService, times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When creating event should retrieve or create city with city service")
        public void whenCreatingEventShouldRetrieveOrCreateCityWithCityService() {
            setupSuccessfulEventCreateMocks();

            eventService.createEvent(eventCreateDto);

            verify(cityService, times(1)).resolve(com.mazurek.eventOrganizer.testData.TestCityData.externalId(CitiesConstants.WARSAW_NAME));
        }

        @Test
        @DisplayName("When creating event should retrieve or create tags with tag service")
        public void whenCreatingEventShouldRetrieveOrCreateTagsWithTagService() {
            setupSuccessfulEventCreateMocks();

            eventService.createEvent(eventCreateDto);

            verify(tagService, times(1)).getTagsByNames(TagConstants.DEFAULT_EVENT_TAGS);
        }

        @Test
        @DisplayName("When creating event should save it with correct data")
        public void whenCreatingEventShouldSaveItWithCorrectData() {
            eventCreateDto = EventCreateDtoTestBuilder.firstEvent()
                    .eventStartDate(TimeConstants.ONE_WEEK_FROM_NOW.plusSeconds(43).plusNanos(987_654_321))
                    .build();
            setupSuccessfulEventCreateMocks();
            ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

            eventService.createEvent(eventCreateDto);

            verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());
            Event capturedEvent = eventArgumentCaptor.getValue();

            assertThat(capturedEvent).isNotNull();
            assertThat(capturedEvent.getCity()).isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedEvent.getName())
                        .as("Should set correct event name")
                        .isEqualTo(eventCreateDto.getName());
                softly.assertThat(capturedEvent.getShortDescription())
                        .as("Should set correct short description")
                        .isEqualTo(eventCreateDto.getShortDescription());
                softly.assertThat(capturedEvent.getLongDescription())
                        .as("Should set correct long description")
                        .isEqualTo(eventCreateDto.getLongDescription());
                softly.assertThat(capturedEvent.getExactAddress())
                        .as("Should set correct exact address")
                        .isEqualTo(eventCreateDto.getExactAddress());
                softly.assertThat(capturedEvent.getCity().getTimeZoneId())
                        .as("Should set correct time zone")
                        .isEqualTo(com.mazurek.eventOrganizer.testData.TestCityData.timeZoneId(eventCreateDto.getCityExternalId()));
                softly.assertThat(capturedEvent.getEventStartDate())
                        .as("Should set correct event start date truncated to minutes")
                        .isEqualTo(TimeConstants.ONE_WEEK_FROM_NOW);
                softly.assertThat(capturedEvent.getCreateDate())
                        .as("Create date should use application clock")
                        .isEqualTo(TimeConstants.NOW);
                softly.assertThat(capturedEvent.getLastUpdate())
                        .as("Last update should use application clock")
                        .isEqualTo(TimeConstants.NOW);
                softly.assertThat(capturedEvent.getCreateDate())
                        .as("Create date and last update should be equal on creation")
                        .isEqualTo(capturedEvent.getLastUpdate());
                softly.assertThat(capturedEvent.getOwner())
                        .as("Should set correct event owner")
                        .isEqualTo(firstUser);
                softly.assertThat(capturedEvent.getMaxAttendees()).isEqualTo(eventCreateDto.getMaxAttendees());
                softly.assertThat(capturedEvent.getAttendeeCount()).isZero();
                softly.assertThat(capturedEvent.getAttendees()).isEmpty();
                softly.assertThat(capturedEvent.getCity())
                        .as("Should set correct city")
                        .isEqualTo(cityWarsaw);
                softly.assertThat(capturedEvent.getTags().stream().map(Tag::getName).collect(Collectors.toSet()))
                        .as("Captured event should contain the same tags as create dto")
                        .isEqualTo(eventCreateDto.getTags().stream().map(tag -> tag.toLowerCase(Locale.ROOT)).collect(Collectors.toSet()));
                softly.assertThat(capturedEvent.getTags())
                        .as("New event should contain the same amount of tags as create dto")
                        .hasSize(eventCreateDto.getTags().size());
            });
        }

        @Test
        @DisplayName("When creating event should return event dto with correct data")
        public void whenCreatingEventShouldReturnDtoWithCorrectData() {
            setupSuccessfulEventCreateMocks();

            EventDto output = eventService.createEvent(eventCreateDto);

            assertThat(output).isNotNull();
            assertThat(output.getOwner()).isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(output.getName())
                        .as("Should return correct event name")
                        .isEqualTo(eventCreateDto.getName());
                softly.assertThat(output.getShortDescription())
                        .as("Should return correct short description")
                        .isEqualTo(eventCreateDto.getShortDescription());
                softly.assertThat(output.getLongDescription())
                        .as("Should return correct long description")
                        .isEqualTo(eventCreateDto.getLongDescription());
                softly.assertThat(output.getEventStartDate())
                        .as("Should return correct event start date truncated to minutes")
                        .isEqualTo(eventCreateDto.getEventStartDate().truncatedTo(ChronoUnit.MINUTES));
                softly.assertThat(output.getCityExternalId())
                        .as("Should return correct city")
                        .isEqualTo(eventCreateDto.getCityExternalId());
                softly.assertThat(output.getTags())
                        .as("Should return the same tags as in create dto")
                        .isEqualTo(eventCreateDto.getTags().stream().map(tag -> tag.toLowerCase(Locale.ROOT)).collect(Collectors.toSet()));
                softly.assertThat(output.getTags())
                        .as("Should return the same amount of tags as in create dto")
                        .hasSize(eventCreateDto.getTags().size());
                softly.assertThat(output.getOwner().getId())
                        .as("Should return correct event owner id")
                        .isEqualTo(firstUser.getId());
                softly.assertThat(output.getOwner().getFirstName())
                        .as("Should return correct event owner first name")
                        .isEqualTo(firstUser.getFirstName());
                softly.assertThat(output.getOwner().getLastName())
                        .as("Should return correct event owner last name")
                        .isEqualTo(firstUser.getLastName());
            });
        }
    }

    @Nested
    @DisplayName("Update event tests:")
    class UpdateEventTests {

        private Set<Tag> updatedTags;

        @BeforeEach
        void setUp() {
            updatedEventDto = EventCreateDtoTestBuilder.updatedEvent().build();
            cityKrakow = CityTestBuilder.krakow().build();
            updatedTags = new HashSet<>(Set.of(tagTwo, tagThree));
        }

        private void setupSuccessfulEventUpdateMocks() {
            when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(cityService.resolve(com.mazurek.eventOrganizer.testData.TestCityData.externalId(CitiesConstants.KRAKOW_NAME))).thenReturn(cityKrakow);
            when(tagService.getTagsByNames(updatedEventDto.getTags())).thenReturn(updatedTags);
            when(eventRepository.save(event)).thenReturn(event);
            when(eventRepository.findAttendeeIdsByEventId(EventConstants.FIRST_EVENT_ID)).thenReturn(List.of(secondUser.getId()));
        }

        @Test
        @DisplayName("When updating event should load event with a write lock")
        public void whenUpdatingEventShouldLoadEventWithWriteLock() {
            setupSuccessfulEventUpdateMocks();

            eventService.updateEvent(updatedEventDto, EventConstants.FIRST_EVENT_ID);

            verify(eventRepository, times(1)).findByIdForUpdate(EventConstants.FIRST_EVENT_ID);
            verify(eventRepository, never()).findById(EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When updating event should throw EventNotFoundException if event does not exist")
        public void whenUpdatingEventShouldThrowEventNotFoundExceptionIfEventDoesNotExist() {
            when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.updateEvent(updatedEventDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);

            verify(eventRepository, never()).findById(EventConstants.FIRST_EVENT_ID);
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("When updating event should throw EventAlreadyHadPlaceException if event start date is in the past")
        public void whenUpdatingEventShouldThrowEventAlreadyHadPlaceExceptionIfEventStartDateIsInThePast() {
            event.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
            when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);

            assertThatThrownBy(() -> eventService.updateEvent(updatedEventDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(EventAlreadyHadPlaceException.class);

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("When updating event should retrieve performing user using AuthenticationService")
        public void whenUpdatingEventShouldRetrievePerformingUserUsingAuthenticationService() {
            setupSuccessfulEventUpdateMocks();

            eventService.updateEvent(updatedEventDto, EventConstants.FIRST_EVENT_ID);

            verify(authenticationService, times(1)).getCurrentUser();
        }

        @Test
        @DisplayName("When updating event should throw NotEventOwnerException if performing user does not own the event")
        public void whenUpdatingEventShouldThrowNotEventOwnerExceptionIfPerformingUserDoesNotOwnTheEvent() {
            when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);

            assertThatThrownBy(() -> eventService.updateEvent(updatedEventDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(NotEventOwnerException.class);

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("When updating event should save it with all updated fields")
        public void whenUpdatingEventShouldSaveItWithAllUpdatedFields() {
            event.setCreateDate(TimeConstants.ONE_WEEK_AGO);
            event.setLastUpdate(TimeConstants.ONE_HOUR_AGO);
            updatedEventDto = EventCreateDtoTestBuilder.updatedEvent()
                    .eventStartDate(TimeConstants.EVENT_UPDATE_START_DATE.plusSeconds(30).plusNanos(123_456_789))
                    .build();
            setupSuccessfulEventUpdateMocks();
            ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

            eventService.updateEvent(updatedEventDto, EventConstants.FIRST_EVENT_ID);

            verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());
            Event capturedEvent = eventArgumentCaptor.getValue();

            assertThat(capturedEvent).isNotNull();
            assertThat(capturedEvent.getCity()).isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedEvent.getName())
                        .as("Should update event name")
                        .isEqualTo(updatedEventDto.getName());
                softly.assertThat(capturedEvent.getShortDescription())
                        .as("Should update short description")
                        .isEqualTo(updatedEventDto.getShortDescription());
                softly.assertThat(capturedEvent.getLongDescription())
                        .as("Should update long description")
                        .isEqualTo(updatedEventDto.getLongDescription());
                softly.assertThat(capturedEvent.getExactAddress())
                        .as("Should update exact address")
                        .isEqualTo(updatedEventDto.getExactAddress());
                softly.assertThat(capturedEvent.getCity().getTimeZoneId())
                        .as("Should update time zone id")
                        .isEqualTo(com.mazurek.eventOrganizer.testData.TestCityData.timeZoneId(updatedEventDto.getCityExternalId()));
                softly.assertThat(capturedEvent.getEventStartDate())
                        .as("Should update event start date truncated to minutes")
                        .isEqualTo(TimeConstants.EVENT_UPDATE_START_DATE);
                softly.assertThat(capturedEvent.getCity())
                        .as("Should update city to the one returned by city service")
                        .isEqualTo(cityKrakow);
                softly.assertThat(capturedEvent.getLastUpdate())
                        .as("Should update lastUpdate using application clock")
                        .isEqualTo(TimeConstants.NOW);
                softly.assertThat(capturedEvent.getCreateDate()).isEqualTo(TimeConstants.ONE_WEEK_AGO);
                softly.assertThat(capturedEvent.getOwner()).isSameAs(firstUser);
                softly.assertThat(capturedEvent.getMaxAttendees()).isEqualTo(updatedEventDto.getMaxAttendees());
            });
        }

        @Test
        @DisplayName("When updating event to unlimited capacity should allow the change")
        public void whenUpdatingEventToUnlimitedCapacityShouldAllowTheChange() {
            event.addAttendee(secondUser);
            updatedEventDto.setMaxAttendees(null);
            setupSuccessfulEventUpdateMocks();

            eventService.updateEvent(updatedEventDto, EventConstants.FIRST_EVENT_ID);

            assertThat(event.getMaxAttendees()).isNull();
            verify(eventRepository).save(event);
        }

        @Test
        @DisplayName("When updating event should retrieve tags from tag service and set them on event")
        public void whenUpdatingEventShouldRetrieveTagsFromTagServiceAndSetThemOnEvent() {
            setupSuccessfulEventUpdateMocks();
            ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

            eventService.updateEvent(updatedEventDto, EventConstants.FIRST_EVENT_ID);

            verify(tagService, times(1)).getTagsByNames(updatedEventDto.getTags());
            verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());

            assertThat(eventArgumentCaptor.getValue().getTags())
                    .as("Event tags should be replaced with the tags returned by tag service")
                    .isEqualTo(updatedTags);
        }

        @Test
        @DisplayName("When updating event should notify event attendees about event update")
        void whenUpdatingEventShouldNotifyEventAttendeesAboutEventUpdate() {
            setupSuccessfulEventUpdateMocks();

            eventService.updateEvent(updatedEventDto, EventConstants.FIRST_EVENT_ID);

            verify(notificationCommandService).notifyEventUpdated(
                    EventConstants.FIRST_EVENT_ID,
                    List.of(secondUser.getId()),
                    EventConstants.EVENT_UPDATE_NAME
            );
        }

        @Test
        @DisplayName("When updating event without attendees should have no notification recipients")
        void whenUpdatingEventWithoutAttendeesShouldHaveNoRecipients() {
            setupSuccessfulEventUpdateMocks();
            when(eventRepository.findAttendeeIdsByEventId(EventConstants.FIRST_EVENT_ID)).thenReturn(List.of());

            eventService.updateEvent(updatedEventDto, EventConstants.FIRST_EVENT_ID);

            verify(notificationCommandService).notifyEventUpdated(EventConstants.FIRST_EVENT_ID,
                    List.of(), EventConstants.EVENT_UPDATE_NAME);
        }

    }

    @Nested
    @DisplayName("Event attendance tests:")
    class EventAttendanceTests {

        @Nested
        @DisplayName("Add attendee to event tests:")
        class AddAttendeeToEventTests {

            private void setupSuccessfulAttendeeAddingMocks() {
                when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
                when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            }

            @Test
            @DisplayName("When adding attendee should load and lock event from database")
            public void whenAddingAttendeeShouldLoadAndLockEventFromDatabase() {
                setupSuccessfulAttendeeAddingMocks();

                eventService.addAttendeeToEvent(EventConstants.FIRST_EVENT_ID);

                verify(eventRepository, times(1)).findByIdForUpdate(EventConstants.FIRST_EVENT_ID);
                verify(eventRepository, never()).findById(any(UUID.class));
            }

            @Test
            @DisplayName("When adding attendee should throw EventNotFoundException if event with given id does not exist")
            public void whenAddingAttendeeShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
                when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> eventService.addAttendeeToEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(EventNotFoundException.class);

                verify(eventRepository).findByIdForUpdate(EventConstants.FIRST_EVENT_ID);
                verify(eventRepository, never()).findById(any(UUID.class));

                verify(eventRepository, never()).save(any(Event.class));
            }

            @Test
            @DisplayName("When adding attendee should throw EventAlreadyHadPlaceException if event start date is in the past")
            public void whenAddingAttendeeShouldThrowEventAlreadyHadPlaceExceptionIfEventStartDateIsInThePast() {
                event.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
                when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);

                assertThatThrownBy(() -> eventService.addAttendeeToEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(EventAlreadyHadPlaceException.class);

                verify(eventRepository, never()).save(any(Event.class));
            }

            @Test
            @DisplayName("When adding attendee should retrieve performing user from AuthenticationService")
            public void whenAddingAttendeeShouldRetrievePerformingUserFromAuthenticationService() {
                setupSuccessfulAttendeeAddingMocks();

                eventService.addAttendeeToEvent(EventConstants.FIRST_EVENT_ID);

                verify(authenticationService, times(1)).getCurrentUser();
            }

            @Test
            @DisplayName("When adding attendee should throw EventOwnerAlreadyAttendsEventException if event owner performs attend action")
            public void whenAddingAttendeeShouldThrowEventOwnerAlreadyAttendsEventExceptionIfEventOwnerPerformsAttendAction() {
                when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
                when(authenticationService.getCurrentUser()).thenReturn(firstUser);

                assertThatThrownBy(() -> eventService.addAttendeeToEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(EventOwnerAlreadyAttendsEventException.class);

                verify(eventRepository, never()).save(any(Event.class));
            }

            @Test
            @DisplayName("When adding attendee should throw AlreadyAttendingEventException if performing user is already attending event")
            public void whenAddingAttendeeShouldThrowAlreadyAttendingEventExceptionIfPerformingUserIsAlreadyAttendingEvent() {
                event.addAttendee(secondUser);
                setupSuccessfulAttendeeAddingMocks();

                assertThatThrownBy(() -> eventService.addAttendeeToEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(AlreadyAttendingEventException.class);

                verify(eventRepository, never()).save(any(Event.class));
            }

            @Test
            @DisplayName("When adding attendee should reject a full event")
            public void whenAddingAttendeeShouldRejectFullEvent() {
                event.setMaxAttendees(1);
                event.addAttendee(secondUser);
                when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
                when(authenticationService.getCurrentUser()).thenReturn(UserTestBuilder.thirdUser().build());

                assertThatThrownBy(() -> eventService.addAttendeeToEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(EventCapacityReachedException.class);

                verify(eventRepository, never()).save(any(Event.class));
                assertThat(event.getAttendees()).containsExactly(secondUser);
                assertThat(event.getAttendeeCount()).isEqualTo(1);
                assertThat(event.getMaxAttendees()).isEqualTo(1);
            }

            @Test
            @DisplayName("When adding attendee to an unlimited event should not apply a capacity check")
            public void whenAddingAttendeeToUnlimitedEventShouldNotApplyCapacityCheck() {
                event.setMaxAttendees(null);
                event.addAttendee(secondUser);
                when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
                User thirdUser = UserTestBuilder.thirdUser().build();
                when(authenticationService.getCurrentUser()).thenReturn(thirdUser);

                eventService.addAttendeeToEvent(EventConstants.FIRST_EVENT_ID);

                verify(eventRepository).save(event);
                assertThat(event.getAttendeeCount()).isEqualTo(2);
                assertThat(event.getAttendees()).containsExactlyInAnyOrder(secondUser, thirdUser);
                assertThat(event.getMaxAttendees()).isNull();
            }

            @Test
            @DisplayName("When adding attendee should add performing user to event attendees and save event")
            public void whenAddingAttendeeShouldAddPerformingUserToEventAttendeesAndSaveEvent() {
                setupSuccessfulAttendeeAddingMocks();
                ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

                eventService.addAttendeeToEvent(EventConstants.FIRST_EVENT_ID);

                verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());

                assertThat(eventArgumentCaptor.getValue().getAttendees())
                        .as("Saved event should contain the performing user in its attending users")
                        .containsExactly(secondUser);
                assertThat(eventArgumentCaptor.getValue().getAttendeeCount()).isEqualTo(1);
            }
        }

        @Nested
        @DisplayName("Remove attendee from event tests:")
        class RemoveAttendeeFromEventTests {

            @BeforeEach
            void setUp() {
                event.addAttendee(secondUser);
            }

            private void setupSuccessfulAttendeeRemovingMocks() {
                when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
                when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            }

            @Test
            @DisplayName("When removing attendee from event should load event with a write lock")
            public void whenRemovingAttendeeFromEventShouldLoadEventWithWriteLock() {
                setupSuccessfulAttendeeRemovingMocks();

                eventService.removeAttendeeFromEvent(EventConstants.FIRST_EVENT_ID);

                verify(eventRepository, times(1)).findByIdForUpdate(EventConstants.FIRST_EVENT_ID);
                verify(eventRepository, never()).findById(EventConstants.FIRST_EVENT_ID);
            }

            @Test
            @DisplayName("When removing attendee from event should throw EventNotFoundException if event with given id does not exist")
            public void whenRemovingAttendeeFromEventShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
                when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> eventService.removeAttendeeFromEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(EventNotFoundException.class);

                verify(eventRepository, never()).findById(EventConstants.FIRST_EVENT_ID);
                verify(eventRepository, never()).save(any(Event.class));
                verify(userRepository, never()).save(any(User.class));
            }

            @Test
            @DisplayName("When removing attendee from event should throw EventAlreadyHadPlaceException if event had place")
            public void whenRemovingAttendeeFromEventShouldThrowEventAlreadyHadPlaceExceptionIfEventHadPlace() {
                event.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
                when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);

                assertThatThrownBy(() -> eventService.removeAttendeeFromEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(EventAlreadyHadPlaceException.class);

                verify(eventRepository, never()).save(any(Event.class));
                verify(userRepository, never()).save(any(User.class));
            }

            @Test
            @DisplayName("When removing attendee from event should retrieve performing user from AuthenticationService")
            public void whenRemovingAttendeeFromEventShouldRetrievePerformingUserFromAuthenticationService() {
                setupSuccessfulAttendeeRemovingMocks();

                eventService.removeAttendeeFromEvent(EventConstants.FIRST_EVENT_ID);

                verify(authenticationService, times(1)).getCurrentUser();
            }

            @Test
            @DisplayName("When removing attendee from event should throw EventOwnerMustAttendEventException if event owner performs remove action")
            public void whenRemovingAttendeeFromEventShouldThrowEventOwnerMustAttendEventExceptionIfEventOwnerPerformsRemoveAction() {
                when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
                when(authenticationService.getCurrentUser()).thenReturn(firstUser);

                assertThatThrownBy(() -> eventService.removeAttendeeFromEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(EventOwnerMustAttendEventException.class);

                verify(eventRepository, never()).save(any(Event.class));
                verify(userRepository, never()).save(any(User.class));
            }

            @Test
            @DisplayName("When removing attendee from event should throw NotEventAttendeeException if performing user is not attending event")
            public void whenRemovingAttendeeFromEventShouldThrowNotEventAttendeeExceptionIfPerformingUserIsNotAttendingEvent() {
                event.removeAttendee(secondUser);
                when(eventRepository.findByIdForUpdate(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
                when(authenticationService.getCurrentUser()).thenReturn(secondUser);

                assertThatThrownBy(() -> eventService.removeAttendeeFromEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(NotEventAttendeeException.class);

                verify(eventRepository, never()).save(any(Event.class));
                verify(userRepository, never()).save(any(User.class));
                assertThat(event.getAttendees()).isEmpty();
                assertThat(event.getAttendeeCount()).isZero();
            }

            @Test
            @DisplayName("When removing attendee from event should remove performing user from event attendees")
            public void whenRemovingAttendeeFromEventShouldRemovePerformingUserFromEventAttendees() {
                setupSuccessfulAttendeeRemovingMocks();

                eventService.removeAttendeeFromEvent(EventConstants.FIRST_EVENT_ID);

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(event.isUserAttendeeOrOwner(secondUser))
                            .as("Performing user should be removed from event attendees")
                            .isFalse();
                    softly.assertThat(event.getAttendees()).doesNotContain(secondUser);
                    softly.assertThat(event.getAttendeeCount()).isZero();
                });
            }

            @Test
            @DisplayName("When removing attendee from event should save only the owning event")
            public void whenRemovingAttendeeFromEventShouldSaveOnlyEvent() {
                setupSuccessfulAttendeeRemovingMocks();

                eventService.removeAttendeeFromEvent(EventConstants.FIRST_EVENT_ID);

                verify(eventRepository, times(1)).save(event);
                verify(userRepository, never()).save(any(User.class));
            }
        }
    }
}
