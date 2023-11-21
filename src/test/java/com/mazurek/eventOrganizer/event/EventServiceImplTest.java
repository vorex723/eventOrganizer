package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.event.dto.EventCreateDto;
import com.mazurek.eventOrganizer.event.mapper.EventMapper;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.exception.event.WrongEventOwnerException;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.thread.dto.ThreadCreateDto;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    public static final String JWT_STRING = "randomStringForJwt";

    private EventService eventService;

    @Mock private EventRepository eventRepository;
    @Mock private CityRepository cityRepository;
    @Mock private CityUtils cityUtils;
    @Mock private UserRepository userRepository;
    @Mock  private EventMapper eventMapper;
    @Mock private TagRepository tagRepository;

    @Mock private JwtUtil jwtUtil;
    private User eventOwner;
    private User secondUser;
    private BCryptPasswordEncoder passwordEncoder = Mockito.spy(new BCryptPasswordEncoder());
    private Optional<Event> eventOptional;
    private EventCreateDto eventCreateDto;
    private Tag tagJava;
    private Tag tagSpring;
    private City cityRzeszow;
    private EventCreateDto updatedEventDto;

    private Tag tagWitam;
    private Tag tagZegnam;
    private City cityKrakow;

    private ThreadCreateDto threadCreateDto;


    private static final String EVENT_OWNER_FIRST_NAME = "Andrew";
    private static final String EVENT_OWNER_LAST_NAME = "Golota";
    private static final String PASSWORD_DEFAULT = "password";
    private static final String EVENT_SHORT_DESCRIPTION = "short description should be short";
    private static final String EVENT_LONG_DESCRIPTION = "long description can be quite long, and it Should be. maybe i should put Lorem Ipsum here.";
    private static final String EVENT_NAME = "First Event";
    private static final String EVENT_EXACT_ADDRESS = "ul. Dąbrowskiego 3";
    private static final String FIRST_THREAD_NAME = "First thread ever" ;
    private static final String FIRST_THREAD_CONTENT = "First thread content for testing purpose. It have to be containing several words.";


    /*
     ********************************************************************************************************************
     *                                       SETTING UP DATA FOR TESTS
     ********************************************************************************************************************
     */

    @BeforeEach
    void setUp() {
        eventService = new EventServiceImpl(eventRepository, cityRepository, cityUtils, tagRepository, userRepository, eventMapper, jwtUtil );

        tagJava = Tag.builder()
                .name("java")
                .id(1L)
                .events(new HashSet<>())
                .build();
        tagSpring = Tag.builder()
                .name("spring")
                .id(2L)
                .events(new HashSet<>())
                .build();

        cityRzeszow = City.builder()
                .id(1L)
                .name("Rzeszow")
                .events(new ArrayList<>())
                .residents(new HashSet<>())
                .build();

        eventOwner =  User.builder()
                .id(1L)
                .email("example@dot.com")
                .role(Role.USER)
                .firstName(EVENT_OWNER_FIRST_NAME)
                .lastName(EVENT_OWNER_LAST_NAME)
                .homeCity(cityRzeszow)
                .attendingEvents(new ArrayList<>())
                .userEvents(new ArrayList<>())
                .password(passwordEncoder.encode(PASSWORD_DEFAULT))
                .lastCredentialsChangeTime(Calendar.getInstance().getTimeInMillis())
                .build();

        secondUser = User.builder()
                .id(2L)
                .role(Role.USER)
                .firstName()
                .lastName("wesoly")
                .homeCity(cityRzeszow)
                .email("notExample@dot.com")
                .userEvents(new ArrayList<>())
                .attendingEvents(new ArrayList<>())
                .build();

        eventOptional = Optional.of(Event.builder()
                        .id(1L)
                        .name(EVENT_NAME)
                        .shortDescription(EVENT_SHORT_DESCRIPTION)
                        .longDescription(EVENT_LONG_DESCRIPTION)
                        .createDate(new Date(Calendar.getInstance().getTimeInMillis()))
                        .tags(new HashSet<>())
                        .attendingUsers(new HashSet<>())
                        .city(cityRzeszow)
                        .exactAddress(EVENT_EXACT_ADDRESS)
                .build());

        eventCreateDto = EventCreateDto.builder()
                .shortDescription(EVENT_SHORT_DESCRIPTION)
                .longDescription(EVENT_LONG_DESCRIPTION)
                .tags(new ArrayList<>())
                .city("Rzeszow")
                .exactAddress(EVENT_EXACT_ADDRESS)
                .build();
        eventCreateDto.getTags().add("java");
        eventCreateDto.getTags().add("spring");


        eventOptional.get().setLastUpdate(eventOptional.get().getCreateDate());
        eventOptional.get().setOwner(eventOwner);
        eventOptional.get().addAttendingUser(eventOwner);

        eventOptional.get().addTag(tagJava);
        eventOptional.get().addTag(tagSpring);

        updatedEventDto = EventCreateDto.builder()
                .name("changed name")
                .shortDescription("changed short description")
                .longDescription("changed long description")
                .city("Krakow")
                .exactAddress("changed exact address")
                .tags(new ArrayList<>())
                .eventStartDate(new Date(2023,Calendar.DECEMBER,31,17,0,0))
                .build();
        updatedEventDto.getTags().add("witam");
        updatedEventDto.getTags().add("zegnam");

        tagWitam = Tag.builder()
                .id(3L)
                .name("witam")
                .events(new HashSet<>())
                .build();
        tagZegnam = Tag.builder()
                .id(4L)
                .name("zegnam")
                .events(new HashSet<>())
                .build();

        cityKrakow = City.builder()
                .id(2L)
                .name("Krakow")
                .events(new ArrayList<>())
                .residents(new HashSet<>())
                .build();

        threadCreateDto = ThreadCreateDto.builder()
                .name(FIRST_THREAD_NAME)
                .content(FIRST_THREAD_CONTENT)
                .build();
    }
    /*
     ********************************************************************************************************************
     *                                       CREATING EVENT TESTS
     ********************************************************************************************************************
     */
    @Nested
    @DisplayName("Create event tests")
    class CreateEventTest{

        @Test
        @DisplayName("When creating event should create empty set for tags if tag list in dto is empty")
        public void whenCreatingEventShouldCreateEmptySetForTagsIfTagListInDtoIsEmpty(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
            when(cityUtils.resolveCity(eventCreateDto.getCity())).thenReturn(cityRzeszow);

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));
            when(eventRepository.save(any(Event.class))).thenReturn(eventOptional.get());

            ArgumentCaptor<Event> eventArgumentCaptor =  ArgumentCaptor.forClass(Event.class);

            eventCreateDto.getTags().clear();

            eventService.createEvent(eventCreateDto, anyString());

            verify(eventRepository,times(1)).save(eventArgumentCaptor.capture());
            Event capturedEvent = eventArgumentCaptor.getValue();
            assertNotNull(capturedEvent.getTags());
            assertNotNull(capturedEvent.getAttendingUsers());
            assertTrue(capturedEvent.getTags().isEmpty());
            assertTrue(capturedEvent.getAttendingUsers().isEmpty());
        }

        @Test
        @DisplayName("When creating event should create empty set for attempting users")
        public void whenCreatingEventShouldCreateEmptySetForAttemptingUsers(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
            when(cityUtils.resolveCity(eventCreateDto.getCity())).thenReturn(cityRzeszow);

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));
            when(eventRepository.save(any(Event.class))).thenReturn(eventOptional.get());

            ArgumentCaptor<Event> eventArgumentCaptor =  ArgumentCaptor.forClass(Event.class);
            eventCreateDto.getTags().clear();
            eventService.createEvent(eventCreateDto, anyString());

            verify(eventRepository,times(1)).save(eventArgumentCaptor.capture());
            Event capturedEvent = eventArgumentCaptor.getValue();
            assertNotNull(capturedEvent.getAttendingUsers());
            assertTrue(capturedEvent.getAttendingUsers().isEmpty());
        }

        @Test
        @DisplayName("When creating event should pass event object to repository with all data")
        public void whenCreatingEventShouldPassEventObjectToRepositoryWithAllData(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
            when(cityUtils.resolveCity(eventCreateDto.getCity())).thenReturn(cityRzeszow);

            when(tagRepository.findByName("java")).thenReturn(Optional.of(tagJava));
            when(tagRepository.findByName("spring")).thenReturn(Optional.of(tagSpring));

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));
            when(eventRepository.save(any(Event.class))).thenReturn(eventOptional.get());

            ArgumentCaptor<Event> eventArgumentCaptor =  ArgumentCaptor.forClass(Event.class);

            eventService.createEvent(eventCreateDto, anyString());

            verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());
            Event capturedEvent = eventArgumentCaptor.getValue();
            assertEquals(eventCreateDto.getName(),capturedEvent.getName());
            assertEquals(eventCreateDto.getShortDescription(),capturedEvent.getShortDescription());
            assertEquals(eventCreateDto.getLongDescription(),capturedEvent.getLongDescription());
            assertEquals(eventCreateDto.getCity(),capturedEvent.getCity().getName());
            assertEquals(eventCreateDto.getExactAddress(),capturedEvent.getExactAddress());

            ArrayList<String> tagNamesFromCapturedEvent = new ArrayList<>();
            capturedEvent.getTags().forEach(tag -> tagNamesFromCapturedEvent.add(tag.getName()));
            eventCreateDto.getTags().forEach(tagName -> assertTrue(tagNamesFromCapturedEvent.contains(tagName)));

        }

    }



    /*
     ********************************************************************************************************************
     *                                       GETTING EVENT BY ID TESTS
     ********************************************************************************************************************
     */

    @Nested
    @DisplayName("Get event by id test")
    class GettingEventByIdTests{

        @Test
        @DisplayName("When getting event by id should run query once")
        public void whenGettingEventByIdShouldRunQueryOnce(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);

            eventService.getEventById(anyLong());

            verify(eventRepository, times(1)).findById(anyLong());
        }
        @Test
        @DisplayName("When getting event by id should throw event not found exception")
        public void whenGettingEventByIdShouldThrowEvenNotFoundException(){
            when(eventRepository.findById(anyLong())).thenReturn(Optional.empty());

            EventNotFoundException eventNotFoundException = assertThrows(EventNotFoundException.class
                    , () ->  eventService.getEventById(anyLong()));

            verify(eventRepository, times(1)).findById(anyLong());
        }
        @Test
        @DisplayName("When getting event by id should map it to eventDto")
        public void whenGettingEventByIdShouldMapItToEventDto(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);

            eventService.getEventById(anyLong());

            verify(eventMapper, times(1)).mapEventToEventWithUsersDto(any(Event.class));
        }

    }




    /*
     ********************************************************************************************************************
     *                                       UPDATING EVENT TESTS
     ********************************************************************************************************************
     */

    @Nested
    @DisplayName("Update event tests")
    class UpdatingEventTest{

        @DisplayName("When updating event should try to load event from database")
        @Test
        public void whenUpdatingEventShouldTryToLoadEventFromDatabase(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));

            eventService.updateEvent(eventCreateDto, 1L, JWT_STRING);

            verify(eventRepository, times(1)).findById(anyLong());

        }
        @DisplayName("When updating event should check if event with this id is empty")
        @Test
        public void whenUpdatingEventShouldCheckIfEventWithThisIdIsEmpty(){
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);

            when(eventRepository.findById(anyLong())).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));

            eventService.updateEvent(eventCreateDto, 1L, JWT_STRING);

            verify(eventOptionalSpy, times(1)).isEmpty();

        }

        @Test
        @DisplayName("When updating event should throw event not found exception if event does not exist")
        public void whenUpdatingEventShouldThrowEventNotFoundExceptionIfEventDoesNotExist(){
            when(eventRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () -> eventService.updateEvent(eventCreateDto, 1L, JWT_STRING));

        }
        @Test
        @DisplayName("When updating event should extract user email from jwt for ownership check")
        public void whenUpdatingEventShouldExtractUserEmailFromJwtForOwnershipCheck(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));

            eventService.updateEvent(eventCreateDto, 1L, JWT_STRING);
            verify(jwtUtil, times(1)).extractUsername(anyString());

        }
        @Test
        @DisplayName("When updating event should find user with extracted email")
        public void whenUpdatingEventShouldFindUserWithExtractedEmail(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));

            eventService.updateEvent(eventCreateDto, 1L, JWT_STRING);
            verify(userRepository, times(1)).findByEmail(anyString());

        }


        @Test
        @DisplayName("When updating event should retrieve user from optional object")
        public void whenUpdatingEventShouldRetrieveUserFromOptionalObject(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());

            Optional<User> eventOwnerOptionalSpy = Mockito.spy(Optional.of(eventOwner));
            when(userRepository.findByEmail(anyString())).thenReturn(eventOwnerOptionalSpy);

            eventService.updateEvent(eventCreateDto, 1L, JWT_STRING);

            verify(eventOwnerOptionalSpy, times(1)).get();
        }
        @Test
        @DisplayName("When updating event should check if is it owner performing update")
        public void whenUpdatingEventShouldCheckIfIsItOwnerPerformingUpdate(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));

            try {
                eventService.updateEvent(eventCreateDto, 1L, JWT_STRING);
            }
            catch (WrongEventOwnerException eventOwnerException){
                fail("Request performer have to be owner of the event");
            }

        }
        @Test
        @DisplayName("When updating event should throw wrong event owner exception if user try to modify not his event")
        public void whenUpdatingEventShouldThrowWrongEventOwnerExceptionIfUserTryToModifyNotHisEvent(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(secondUser));

            assertThrows(WrongEventOwnerException.class, () -> eventService.updateEvent(eventCreateDto, 1L, JWT_STRING));
        }

        @Test
        @DisplayName("When updating Event should update basic fields of event")
        public void whenUpdatingEventShouldUpdateFieldsOfEvent(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));

            when(tagRepository.findByName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);

            Event eventToBeSaved = eventOptional.get();

            eventService.updateEvent(updatedEventDto, 1L, JWT_STRING);



            assertEquals(updatedEventDto.getName(), eventToBeSaved.getName());
            assertEquals(updatedEventDto.getShortDescription(),eventToBeSaved.getShortDescription());
            assertEquals(updatedEventDto.getLongDescription(),eventToBeSaved.getLongDescription());
            assertEquals(updatedEventDto.getCity(),eventToBeSaved.getCity().getName());
            assertEquals(updatedEventDto.getExactAddress(),eventToBeSaved.getExactAddress());
            assertEquals(updatedEventDto.getEventStartDate(),eventToBeSaved.getEventStartDate());
        }

        @Test
        @DisplayName("When updating event should remove tags not appearing in dto from event list of tags")
        public void whenUpdatingEventShouldRemoveTagsNotAppearingInDtoFromEventListOfTags(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));

            when(tagRepository.findByName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);

            Event eventToBeSaved = eventOptional.get();

            eventService.updateEvent(updatedEventDto, 1L, JWT_STRING);

            assertFalse(eventToBeSaved.getTags().contains(tagJava));
            assertFalse(tagJava.getEvents().contains(eventOptional.get()));

            assertFalse(eventToBeSaved.getTags().contains(tagSpring));
            assertFalse(tagSpring.getEvents().contains(eventOptional.get()));

            assertTrue(eventToBeSaved.getTags().contains(tagZegnam));
            assertTrue(tagZegnam.getEvents().contains(eventOptional.get()));

            assertTrue(eventToBeSaved.getTags().contains(tagWitam));
            assertTrue(tagWitam.getEvents().contains(eventOptional.get()));





        }
        @Test
        @DisplayName("When updating event should not remove tags appearing in dto from event list of tags")
        public void whenUpdatingEventShouldNotRemoveTagsAppearingInDtoFromEventListOfTags(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));

            Event event = eventOptional.get();

            updatedEventDto.getTags().removeIf(tag -> tag.equals("witam"));
            updatedEventDto.getTags().add("java");

            when(tagRepository.findByName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);

            eventService.updateEvent(updatedEventDto, 1L, JWT_STRING);

            Event eventToBeSaved = eventOptional.get();


            assertTrue(eventToBeSaved.getTags().contains(tagJava));
            assertTrue(tagJava.getEvents().contains(eventOptional.get()));

            assertFalse(eventToBeSaved.getTags().contains(tagSpring));
            assertFalse(tagSpring.getEvents().contains(eventOptional.get()));

            assertTrue(eventToBeSaved.getTags().contains(tagZegnam));
            assertTrue(tagZegnam.getEvents().contains(eventOptional.get()));

            assertFalse(eventToBeSaved.getTags().contains(tagWitam));
            assertFalse(tagWitam.getEvents().contains(eventOptional.get()));


        }
        @Test
        @DisplayName("When updating event should remove all tags if dto tag list is empty from event list of tags")
        public void whenUpdatingEventShouldRemoveAllTagsIfDtoTagListIsEmptyFromEventListOfTags(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));


            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);

            updatedEventDto.getTags().clear();


            eventService.updateEvent(updatedEventDto, 1L, JWT_STRING);

            Event eventToBeSaved = eventOptional.get();

            assertFalse(eventToBeSaved.getTags().contains(tagJava));
            assertFalse(tagJava.getEvents().contains(eventOptional.get()));

            assertFalse(eventToBeSaved.getTags().contains(tagSpring));
            assertFalse(tagSpring.getEvents().contains(eventOptional.get()));

            assertFalse(eventToBeSaved.getTags().contains(tagZegnam));
            assertFalse(tagZegnam.getEvents().contains(eventOptional.get()));

            assertFalse(eventToBeSaved.getTags().contains(tagWitam));
            assertFalse(tagWitam.getEvents().contains(eventOptional.get()));

            assertTrue(eventToBeSaved.getTags().isEmpty());

        }

        @Test
        @DisplayName("When updating event should add lacking tags from dto tag list to event tag list")
        public void whenUpdatingEventShouldAddLackingTagsFromDtoTagListToEventTagList(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));

            when(tagRepository.findByName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);

            updatedEventDto.getTags().add("java");
            updatedEventDto.getTags().add("spring");

            eventService.updateEvent(updatedEventDto, 1L, JWT_STRING);

            Event eventToBeSaved = eventOptional.get();

            assertTrue(eventToBeSaved.getTags().contains(tagJava));
            assertTrue(tagJava.getEvents().contains(eventOptional.get()));

            assertTrue(eventToBeSaved.getTags().contains(tagSpring));
            assertTrue(tagSpring.getEvents().contains(eventOptional.get()));

            assertTrue(eventToBeSaved.getTags().contains(tagZegnam));
            assertTrue(tagZegnam.getEvents().contains(eventOptional.get()));

            assertTrue(eventToBeSaved.getTags().contains(tagWitam));
            assertTrue(tagWitam.getEvents().contains(eventOptional.get()));

        }
        @Test
        @DisplayName("When updating event should save changes to database")
        public void whenUpdatingEventShouldSaveChangesToDatabase(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));

            when(tagRepository.findByName("witam")).thenReturn(Optional.of(tagWitam));
            when(tagRepository.findByName("zegnam")).thenReturn(Optional.of(tagZegnam));
            when(cityUtils.resolveCity("Krakow")).thenReturn(cityKrakow);


            eventService.updateEvent(updatedEventDto, 1L, JWT_STRING);

            Event eventToBeSaved = eventOptional.get();
            verify(eventRepository, times(1)).save(any(Event.class));

        }

    }

    /*
     ********************************************************************************************************************
     *                                       ADDING ATTENDER TO EVENT TESTS
     ********************************************************************************************************************
     */

    @Nested
    @DisplayName("Adding attender to event tests")
    class AddingAttenderToEventTests{

        @Test
        @DisplayName("When adding attender should try to load event from database")
        public void whenAddingAttenderShouldTryToLoadEventFromDatabase(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(secondUser.getUsername());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(1L, JWT_STRING);

            verify(eventRepository, times(1)).findById(anyLong());

        }
        @Test
        @DisplayName("When adding attender should check if event optional is empty")
        public void whenAddingAttenderShouldCheckIfEventIsPresent(){
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);
            when(eventRepository.findById(anyLong())).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(secondUser.getUsername());
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(1L,JWT_STRING);

            verify(eventOptionalSpy,times(1)).isEmpty();
        }

        @Test
        @DisplayName("When adding attender should throw EventNotFound exception if event optional is empty")
        public void whenAddingAttenderShouldThrowEventNotFoundExceptionIfEventOptionalIsEmpty(){
            when(eventRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class,() -> eventService.addAttenderToEvent(1L, JWT_STRING));
        }

        @Test
        @DisplayName("When adding attender should extract username from jwt")
        public void whenAddingAttenderShouldExtractUsernameFromJwt(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(secondUser.getUsername());

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(1L, JWT_STRING);

            verify(jwtUtil,times(1)).extractUsername(JWT_STRING);

        }

        @Test
        @DisplayName("When adding attender should load user from database with extracted from jwt username")
        public void whenAddingAttenderShouldLoadUserFromDatabaseWithExtractedFromJwtUsername(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(secondUser.getUsername());
            when(userRepository.findByEmail(secondUser.getEmail())).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(1L, JWT_STRING);

        }

        @Test
        @DisplayName("When adding attender should retrieve user from optional object")
        public void whenAddingAttenderShouldRetrieveUserFromOptionalObject(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(secondUser.getUsername());
            Optional<User> secondUserOptionalSpy = Mockito.spy(Optional.of(secondUser));
            when(userRepository.findByEmail(secondUser.getEmail())).thenReturn(secondUserOptionalSpy);

            eventService.addAttenderToEvent(1L, JWT_STRING);

            verify(secondUserOptionalSpy,times(1)).get();

        }
        @Test
        @DisplayName("When adding attender should retrieve event from optional object")
        public void whenAddingAttenderShouldRetrieveEventFromOptionalObject(){
            Optional<Event> eventOptionalSpy = Mockito.spy(eventOptional);
            when(eventRepository.findById(anyLong())).thenReturn(eventOptionalSpy);
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(secondUser.getUsername());
            when(userRepository.findByEmail(secondUser.getEmail())).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(1L, JWT_STRING);

            verify(eventOptionalSpy,times(1)).get();

        }
        @Test
        @DisplayName("When adding attender should add retrieved user to event attender list")
        public void whenAddingAttenderShouldAddRetrievedUserToEventAttenderList(){
            Event eventSpy = Mockito.spy(eventOptional.get());
            when(eventRepository.findById(anyLong())).thenReturn(Optional.of(eventSpy));
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(secondUser.getUsername());
            when(userRepository.findByEmail(secondUser.getEmail())).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(1L, JWT_STRING);

            verify(eventSpy,times(1)).addAttendingUser(secondUser);

            assertTrue(secondUser.getAttendingEvents().contains(eventSpy));
            assertTrue(eventSpy.getAttendingUsers().contains(secondUser));

        }

        @Test
        @DisplayName("When adding attender should save changes to database")
        public void whenAddingAttenderShouldSaveChangesToDatabase(){
            Event eventSpy = Mockito.spy(eventOptional.get());
            when(eventRepository.findById(anyLong())).thenReturn(Optional.of(eventSpy));
            when(jwtUtil.extractUsername(JWT_STRING)).thenReturn(secondUser.getUsername());
            when(userRepository.findByEmail(secondUser.getEmail())).thenReturn(Optional.of(secondUser));

            eventService.addAttenderToEvent(1L, JWT_STRING);

            verify(eventRepository, times(1)).save(any(Event.class));

        }

    }

    /*
     ********************************************************************************************************************
     *                                       CREATING THREAD IN EVENT
     ********************************************************************************************************************
     */
    @Nested
    @DisplayName("Thread create in event tests:")
    class ThreadCreateTests{

        @Test
        @DisplayName("When creating thread should try to load event from database")
        public void whenCreatingThreadShouldTryToLoadEventFromDatabase(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);

            eventService.createThreadInEvent(threadCreateDto,1L, JWT_STRING);

            verify(eventRepository, times(1)).findById(anyLong());
        }
        @Test
        @DisplayName("When creating thread should check if returned event optional is not empty")
        public void whenCreatingThreadShouldCheckIfReturnedEventOptionIsNotEmpty(){
            Optional<Event> eventOptionalSpy = Mockito.spy();
            when(eventRepository.findById(anyLong())).thenReturn(eventOptionalSpy);

            eventService.createThreadInEvent(threadCreateDto,1L, JWT_STRING);

            verify(eventOptionalSpy, times(1)).isEmpty();
        }
        @Test
        @DisplayName("When creating thread should throw EventNotException if event optional is empty")
        public void whenCreatingThreadShouldThrowEventNotFoundExceptionIfEventOptionalIsEmpty(){
            when(eventRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(EventNotFoundException.class, () -> eventService.createThreadInEvent(threadCreateDto,1L, JWT_STRING));
        }
        @Test
        @DisplayName("When creating thread should extract user email from jwt")
        public void whenCreatingThreadShould4(){
            when(eventRepository.findById(anyLong())).thenReturn(eventOptional);
            when(jwtUtil.extractUsername(anyString())).thenReturn(secondUser.getEmail());
            eventService.createThreadInEvent(threadCreateDto,1L, JWT_STRING);


        }
        @Test
        @DisplayName("")
        public void whenCreatingThreadShould5(){

        }
        @Test
        @DisplayName("")
        public void whenCreatingThreadShould6(){

        }
        @Test
        @DisplayName("")
        public void whenCreatingThreadShould7(){

        }
        @Test
        @DisplayName("")
        public void whenCreatingThreadShould8(){

        }
        @Test
        @DisplayName("")
        public void whenCreatingThreadShould9(){

        }


    }
}