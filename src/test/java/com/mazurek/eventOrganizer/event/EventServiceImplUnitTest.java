package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.config.properties.PaginationProperties;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.dto.EventDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.event.*;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

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
@ActiveProfiles("test")
@DisplayName("EventServiceImpl unit tests:")
class EventServiceImplUnitTest {

    @InjectMocks
    private EventServiceImpl eventService;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private TagService tagService;
    @Mock
    private CityService cityService;
    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private PaginationProperties paginationProperties;
    @Mock
    private Clock clock;

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
        cityWarsaw.addEvent(event);

        eventCreateDto = EventCreateDtoTestBuilder.firstEvent().build();
        lenient().when(clock.instant()).thenReturn(TimeConstants.NOW);
        lenient().when(paginationProperties.getDefaultPageSize()).thenReturn(PaginationConstants.DEFAULT_PAGE_SIZE);
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
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);

            EventDto output = eventService.getEventById(EventConstants.FIRST_EVENT_ID);

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
                softly.assertThat(output.getAttendingUsers())
                        .as("Should return correct attending users count")
                        .hasSize(event.getAttendingUsers().size());
            });
        }
    }

    @Nested
    @DisplayName("Get events tests:")
    class GetEventsTests {

        @Test
        @DisplayName("When getting events should use page number and default page size in repository query")
        public void whenGettingEventsShouldUsePageNumberAndDefaultPageSizeInRepositoryQuery() {
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

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedPageable.getPageNumber()).isEqualTo(pageNumber);
                softly.assertThat(capturedPageable.getPageSize()).isEqualTo(PaginationConstants.DEFAULT_PAGE_SIZE);
                softly.assertThat(capturedPageable.getSort().getOrderFor("eventStartDate")).isNotNull();
            });
        }

        @Test
        @DisplayName("When getting events should return empty page if requested page is empty")
        public void whenGettingEventsShouldReturnEmptyPageIfRequestedPageIsEmpty() {
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
            verify(userRepository, times(1)).findById(UserConstants.FIRST_USER_ID);
            verify(eventRepository, times(1))
                    .findUpcomingEventsByOwnerId(eq(UserConstants.FIRST_USER_ID), eq(TimeConstants.NOW), any(Pageable.class));
            verify(eventRepository, never())
                    .findByOwnerId(eq(UserConstants.FIRST_USER_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("When getting user events without upcoming flag should use all-events query")
        public void whenGettingUserEventsWithoutUpcomingFlagShouldUseAllEventsQuery() {
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
            when(cityService.getCityByNameOrCreate(CitiesConstants.WARSAW_NAME)).thenReturn(cityWarsaw);
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

            verify(cityService, times(1)).getCityByNameOrCreate(CitiesConstants.WARSAW_NAME);
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
            setupSuccessfulEventCreateMocks();
            ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

            eventService.createEvent(eventCreateDto);

            verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());
            Event capturedEvent = eventArgumentCaptor.getValue();

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
                softly.assertThat(capturedEvent.getTimeZoneId())
                        .as("Should set correct time zone")
                        .isEqualTo(eventCreateDto.getTimeZone());
                softly.assertThat(capturedEvent.getEventStartDate())
                        .as("Should set correct event start date truncated to minutes")
                        .isEqualTo(eventCreateDto.getEventStartDate().truncatedTo(ChronoUnit.MINUTES));
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
                softly.assertThat(capturedEvent.getCity())
                        .as("Should set correct city")
                        .isEqualTo(cityWarsaw);
                softly.assertThat(capturedEvent.getTags().stream().map(Tag::getName).collect(Collectors.toSet()))
                        .as("Captured event should contain the same tags as create dto")
                        .isEqualTo(eventCreateDto.getTags().stream().map(tag -> tag.toLowerCase(Locale.ROOT)).collect(Collectors.toSet()));
                softly.assertThat(capturedEvent.getTags())
                        .as("New event should contain the same amount of tags as create dto")
                        .hasSize(eventCreateDto.getTags().size());
                softly.assertThat(tagOne.getEvents())
                        .as("Tag one should contain the captured event")
                        .contains(capturedEvent);
                softly.assertThat(tagTwo.getEvents())
                        .as("Tag two should contain the captured event")
                        .contains(capturedEvent);
                softly.assertThat(firstUser.getUserEvents())
                        .as("Event owner should have the captured event added to their events")
                        .contains(capturedEvent);
            });
        }

        @Test
        @DisplayName("When creating event should return event dto with correct data")
        public void whenCreatingEventShouldReturnDtoWithCorrectData() {
            setupSuccessfulEventCreateMocks();

            EventDto output = eventService.createEvent(eventCreateDto);

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
                softly.assertThat(output.getCity().toLowerCase(Locale.ROOT))
                        .as("Should return correct city")
                        .isEqualTo(eventCreateDto.getCity().toLowerCase(Locale.ROOT));
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
                softly.assertThat(output.getOwner().getHomeCity())
                        .as("Should return correct event owner home city")
                        .isEqualTo(firstUser.getHomeCity().getName());
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
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(firstUser);
            when(cityService.getCityByNameOrCreate(CitiesConstants.KRAKOW_NAME)).thenReturn(cityKrakow);
            when(tagService.getTagsByNames(updatedEventDto.getTags())).thenReturn(updatedTags);
            when(eventRepository.save(event)).thenReturn(event);
        }

        @Test
        @DisplayName("When updating event should load event from database")
        public void whenUpdatingEventShouldLoadEventFromDatabase() {
            setupSuccessfulEventUpdateMocks();

            eventService.updateEvent(updatedEventDto, EventConstants.FIRST_EVENT_ID);

            verify(eventRepository, times(1)).findById(EventConstants.FIRST_EVENT_ID);
        }

        @Test
        @DisplayName("When updating event should throw EventNotFoundException if event does not exist")
        public void whenUpdatingEventShouldThrowEventNotFoundExceptionIfEventDoesNotExist() {
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.updateEvent(updatedEventDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(EventNotFoundException.class);

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("When updating event should throw EventAlreadyHadPlaceException if event start date is in the past")
        public void whenUpdatingEventShouldThrowEventAlreadyHadPlaceExceptionIfEventStartDateIsInThePast() {
            event.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);

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
            when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
            when(authenticationService.getCurrentUser()).thenReturn(secondUser);

            assertThatThrownBy(() -> eventService.updateEvent(updatedEventDto, EventConstants.FIRST_EVENT_ID))
                    .isInstanceOf(NotEventOwnerException.class);

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("When updating event should save it with all updated fields")
        public void whenUpdatingEventShouldSaveItWithAllUpdatedFields() {
            setupSuccessfulEventUpdateMocks();
            ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

            eventService.updateEvent(updatedEventDto, EventConstants.FIRST_EVENT_ID);

            verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());
            Event capturedEvent = eventArgumentCaptor.getValue();

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
                softly.assertThat(capturedEvent.getTimeZoneId())
                        .as("Should update time zone id")
                        .isEqualTo(updatedEventDto.getTimeZone());
                softly.assertThat(capturedEvent.getEventStartDate())
                        .as("Should update event start date truncated to minutes")
                        .isEqualTo(updatedEventDto.getEventStartDate().truncatedTo(ChronoUnit.MINUTES));
                softly.assertThat(capturedEvent.getCity())
                        .as("Should update city to the one returned by city service")
                        .isEqualTo(cityKrakow);
                softly.assertThat(capturedEvent.getLastUpdate())
                        .as("Should update lastUpdate using application clock")
                        .isEqualTo(TimeConstants.NOW);
            });
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

    }

    @Nested
    @DisplayName("Event attendance tests:")
    class EventAttendanceTests {

        @Nested
        @DisplayName("Add attender to event tests:")
        class AddAttenderToEventTests {

            private void setupSuccessfulAttenderAddingMocks() {
                when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
                when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            }

            @Test
            @DisplayName("When adding attender should load event from database")
            public void whenAddingAttenderShouldLoadEventFromDatabase() {
                setupSuccessfulAttenderAddingMocks();

                eventService.addAttenderToEvent(EventConstants.FIRST_EVENT_ID);

                verify(eventRepository, times(1)).findById(EventConstants.FIRST_EVENT_ID);
            }

            @Test
            @DisplayName("When adding attender should throw EventNotFoundException if event with given id does not exist")
            public void whenAddingAttenderShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
                when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> eventService.addAttenderToEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(EventNotFoundException.class);

                verify(eventRepository, never()).save(any(Event.class));
            }

            @Test
            @DisplayName("When adding attender should throw EventAlreadyHadPlaceException if event start date is in the past")
            public void whenAddingAttenderShouldThrowEventAlreadyHadPlaceExceptionIfEventStartDateIsInThePast() {
                event.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
                when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);

                assertThatThrownBy(() -> eventService.addAttenderToEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(EventAlreadyHadPlaceException.class);

                verify(eventRepository, never()).save(any(Event.class));
            }

            @Test
            @DisplayName("When adding attender should retrieve performing user from AuthenticationService")
            public void whenAddingAttenderShouldRetrievePerformingUserFromAuthenticationService() {
                setupSuccessfulAttenderAddingMocks();

                eventService.addAttenderToEvent(EventConstants.FIRST_EVENT_ID);

                verify(authenticationService, times(1)).getCurrentUser();
            }

            @Test
            @DisplayName("When adding attender should throw EventOwnerAlreadyAttendsEventException if event owner performs attend action")
            public void whenAddingAttenderShouldThrowEventOwnerAlreadyAttendsEventExceptionIfEventOwnerPerformsAttendAction() {
                when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
                when(authenticationService.getCurrentUser()).thenReturn(firstUser);

                assertThatThrownBy(() -> eventService.addAttenderToEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(EventOwnerAlreadyAttendsEventException.class);

                verify(eventRepository, never()).save(any(Event.class));
            }

            @Test
            @DisplayName("When adding attender should throw AlreadyAttendingEventException if performing user is already attending event")
            public void whenAddingAttenderShouldThrowAlreadyAttendingEventExceptionIfPerformingUserIsAlreadyAttendingEvent() {
                event.addAttendingUser(secondUser);
                secondUser.addAttendingEvent(event);
                setupSuccessfulAttenderAddingMocks();

                assertThatThrownBy(() -> eventService.addAttenderToEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(AlreadyAttendingEventException.class);

                verify(eventRepository, never()).save(any(Event.class));
            }

            @Test
            @DisplayName("When adding attender should add performing user to event attending users and save event")
            public void whenAddingAttenderShouldAddPerformingUserToEventAttendingUsersAndSaveEvent() {
                setupSuccessfulAttenderAddingMocks();
                ArgumentCaptor<Event> eventArgumentCaptor = ArgumentCaptor.forClass(Event.class);

                eventService.addAttenderToEvent(EventConstants.FIRST_EVENT_ID);

                verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());

                assertThat(eventArgumentCaptor.getValue().getAttendingUsers())
                        .as("Saved event should contain the performing user in its attending users")
                        .contains(secondUser);
            }
        }

        @Nested
        @DisplayName("Remove attender from event tests:")
        class RemoveAttenderFromEventTests {

            @BeforeEach
            void setUp() {
                event.addAttendingUser(secondUser);
            }

            private void setupSuccessfulAttenderRemovingMocks() {
                when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
                when(authenticationService.getCurrentUser()).thenReturn(secondUser);
            }

            @Test
            @DisplayName("When removing attender from event should load event with given id from database")
            public void whenRemovingAttenderFromEventShouldLoadEventWithGivenIdFromDatabase() {
                setupSuccessfulAttenderRemovingMocks();

                eventService.removeAttenderFromEvent(EventConstants.FIRST_EVENT_ID);

                verify(eventRepository, times(1)).findById(EventConstants.FIRST_EVENT_ID);
            }

            @Test
            @DisplayName("When removing attender from event should throw EventNotFoundException if event with given id does not exist")
            public void whenRemovingAttenderFromEventShouldThrowEventNotFoundExceptionIfEventWithGivenIdDoesNotExist() {
                when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> eventService.removeAttenderFromEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(EventNotFoundException.class);

                verify(eventRepository, never()).save(any(Event.class));
                verify(userRepository, never()).save(any(User.class));
            }

            @Test
            @DisplayName("When removing attender from event should throw EventAlreadyHadPlaceException if event had place")
            public void whenRemovingAttenderFromEventShouldThrowEventAlreadyHadPlaceExceptionIfEventHadPlace() {
                event.setEventStartDate(TimeConstants.ONE_WEEK_AGO);
                when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);

                assertThatThrownBy(() -> eventService.removeAttenderFromEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(EventAlreadyHadPlaceException.class);

                verify(eventRepository, never()).save(any(Event.class));
                verify(userRepository, never()).save(any(User.class));
            }

            @Test
            @DisplayName("When removing attender from event should retrieve performing user from AuthenticationService")
            public void whenRemovingAttenderFromEventShouldRetrievePerformingUserFromAuthenticationService() {
                setupSuccessfulAttenderRemovingMocks();

                eventService.removeAttenderFromEvent(EventConstants.FIRST_EVENT_ID);

                verify(authenticationService, times(1)).getCurrentUser();
            }

            @Test
            @DisplayName("When removing attender from event should throw EventOwnerMustAttendEventException if event owner performs remove action")
            public void whenRemovingAttenderFromEventShouldThrowEventOwnerMustAttendEventExceptionIfEventOwnerPerformsRemoveAction() {
                when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
                when(authenticationService.getCurrentUser()).thenReturn(firstUser);

                assertThatThrownBy(() -> eventService.removeAttenderFromEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(EventOwnerMustAttendEventException.class);

                verify(eventRepository, never()).save(any(Event.class));
                verify(userRepository, never()).save(any(User.class));
            }

            @Test
            @DisplayName("When removing attender from event should throw NotEventAttenderException if performing user is not attending event")
            public void whenRemovingAttenderFromEventShouldThrowNotEventAttenderExceptionIfPerformingUserIsNotAttendingEvent() {
                event.getAttendingUsers().remove(secondUser);
                secondUser.getUserEvents().remove(event);
                when(eventRepository.findById(EventConstants.FIRST_EVENT_ID)).thenReturn(eventOptional);
                when(authenticationService.getCurrentUser()).thenReturn(secondUser);

                assertThatThrownBy(() -> eventService.removeAttenderFromEvent(EventConstants.FIRST_EVENT_ID))
                        .isInstanceOf(NotEventAttenderException.class);

                verify(eventRepository, never()).save(any(Event.class));
                verify(userRepository, never()).save(any(User.class));
            }

            @Test
            @DisplayName("When removing attender from event should remove performing user from event attending users")
            public void whenRemovingAttenderFromEventShouldRemovePerformingUserFromEventAttendingUsers() {
                setupSuccessfulAttenderRemovingMocks();

                eventService.removeAttenderFromEvent(EventConstants.FIRST_EVENT_ID);

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(event.isUserAttending(secondUser))
                            .as("Performing user should be removed from event attending users")
                            .isFalse();
                    softly.assertThat(secondUser.getAttendingEvents())
                            .as("Event should be removed from performing user attending events")
                            .doesNotContain(event);
                });
            }

            @Test
            @DisplayName("When removing attender from event should save event and user")
            public void whenRemovingAttenderFromEventShouldSaveEventAndUser() {
                setupSuccessfulAttenderRemovingMocks();

                eventService.removeAttenderFromEvent(EventConstants.FIRST_EVENT_ID);

                verify(eventRepository, times(1)).save(event);
                verify(userRepository, times(1)).save(secondUser);
            }
        }
    }
}
