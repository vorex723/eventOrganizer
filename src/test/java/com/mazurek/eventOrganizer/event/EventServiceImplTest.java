package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.event.dto.EventCreationDto;
import com.mazurek.eventOrganizer.event.dto.EventWithUsersDto;
import com.mazurek.eventOrganizer.event.mapper.EventMapper;
import com.mazurek.eventOrganizer.exception.event.EventNotFoundException;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
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

    private EventService eventService;

    @Mock private EventRepository eventRepository;
    @Mock private CityRepository cityRepository;
    @Mock private CityUtils cityUtils;
    @Mock private UserRepository userRepository;
    @Mock  private EventMapper eventMapper;
    @Mock private TagRepository tagRepository;

    @Mock private JwtUtil jwtUtil;
    private User eventOwner;
    private BCryptPasswordEncoder passwordEncoder = Mockito.spy(new BCryptPasswordEncoder());
    private Optional<Event> eventOptional;
    private EventCreationDto eventCreationDto;
    private Tag tagJava;
    private Tag tagSpring;
    private City cityRzeszow;

  /*  @Captor
    private ArgumentCaptor<Event> eventArgumentCaptor =  ArgumentCaptor.forClass(Event.class);*/

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
                .firstName("Andrew")
                .lastName("Golota")
                .homeCity(cityRzeszow)
                .attendingEvents(new ArrayList<>())
                .userEvents(new ArrayList<>())
                .password(passwordEncoder.encode("password"))
                .lastCredentialsChangeTime(Calendar.getInstance().getTimeInMillis())
                .build();

        eventOptional = Optional.of(Event.builder()
                        .id(1L)
                        .shortDescription("short description should be short")
                        .longDescription("long description can be quite long, and it Should be. maybe i should put Lorem Ipsum here.")
                        .createDate(new Date(Calendar.getInstance().getTimeInMillis()))
                        .tags(new HashSet<>())
                        .attendingUsers(new HashSet<>())
                        .city(cityRzeszow)
                        .exactAddress("ul. Dąbrowskiego 3")
                .build());

        eventCreationDto = EventCreationDto.builder()
                .shortDescription("short description should be short")
                .longDescription("long description can be quite long, and it Should be. maybe i should put Lorem Ipsum here.")
                .tags(new ArrayList<>())
                .city("Rzeszow")
                .exactAddress("ul. Dąbrowskiego 3")
                .build();
        eventCreationDto.getTags().add("java");
        eventCreationDto.getTags().add("spring");
        //eventCreationDto.getTags()

        eventOptional.get().setLastUpdate(eventOptional.get().getCreateDate());
        eventOptional.get().setOwner(eventOwner);
        eventOptional.get().addAttendingUser(eventOwner);

        eventOptional.get().addTag(tagJava);
        eventOptional.get().addTag(tagSpring);

    }
    /*
     ********************************************************************************************************************
     *                                       CREATING EVENT TESTS
     ********************************************************************************************************************
     */
    @Test
    public void whenCreatingEventShouldCreateEmptySetForTagsIfTagListInDtoIsEmpty(){
        when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
        when(cityUtils.resolveCity(eventCreationDto.getCity())).thenReturn(cityRzeszow);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));
        when(eventRepository.save(any(Event.class))).thenReturn(eventOptional.get());

        ArgumentCaptor<Event> eventArgumentCaptor =  ArgumentCaptor.forClass(Event.class);

        eventCreationDto.getTags().clear();

        eventService.createEvent(eventCreationDto, anyString());

        verify(eventRepository,times(1)).save(eventArgumentCaptor.capture());
        Event capturedEvent = eventArgumentCaptor.getValue();
        assertNotNull(capturedEvent.getTags());
        assertNotNull(capturedEvent.getAttendingUsers());
        assertTrue(capturedEvent.getTags().isEmpty());
        assertTrue(capturedEvent.getAttendingUsers().isEmpty());
    }

    @Test
    public void whenCreatingEventShouldCreateEmptySetForAttemptingUsers(){
        when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
        when(cityUtils.resolveCity(eventCreationDto.getCity())).thenReturn(cityRzeszow);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));
        when(eventRepository.save(any(Event.class))).thenReturn(eventOptional.get());

        ArgumentCaptor<Event> eventArgumentCaptor =  ArgumentCaptor.forClass(Event.class);
        eventCreationDto.getTags().clear();
        eventService.createEvent(eventCreationDto, anyString());

        verify(eventRepository,times(1)).save(eventArgumentCaptor.capture());
        Event capturedEvent = eventArgumentCaptor.getValue();
        assertNotNull(capturedEvent.getAttendingUsers());
        assertTrue(capturedEvent.getAttendingUsers().isEmpty());
    }

    @Test
    public void whenCreatingEventShouldPassEventObjectToRepositoryWithAllData(){
        when(jwtUtil.extractUsername(anyString())).thenReturn(eventOwner.getEmail());
        when(cityUtils.resolveCity(eventCreationDto.getCity())).thenReturn(cityRzeszow);

        when(tagRepository.findByName("java")).thenReturn(Optional.of(tagJava));
        when(tagRepository.findByName("spring")).thenReturn(Optional.of(tagSpring));

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(eventOwner));
        when(eventRepository.save(any(Event.class))).thenReturn(eventOptional.get());

        ArgumentCaptor<Event> eventArgumentCaptor =  ArgumentCaptor.forClass(Event.class);

        eventService.createEvent(eventCreationDto, anyString());

        verify(eventRepository, times(1)).save(eventArgumentCaptor.capture());
        Event capturedEvent = eventArgumentCaptor.getValue();
        assertEquals(eventCreationDto.getName(),capturedEvent.getName());
        assertEquals(eventCreationDto.getShortDescription(),capturedEvent.getShortDescription());
        assertEquals(eventCreationDto.getLongDescription(),capturedEvent.getLongDescription());
        assertEquals(eventCreationDto.getCity(),capturedEvent.getCity().getName());
        assertEquals(eventCreationDto.getExactAddress(),capturedEvent.getExactAddress());

        ArrayList<String> tagNamesFromCapturedEvent = new ArrayList<>();
        capturedEvent.getTags().forEach(tag -> tagNamesFromCapturedEvent.add(tag.getName()));
        eventCreationDto.getTags().forEach(tagName -> assertTrue(tagNamesFromCapturedEvent.contains(tagName)));

    }


    /*
     ********************************************************************************************************************
     *                                       GETTING EVENT BY ID TESTS
     ********************************************************************************************************************
     */
    @Test
    public void whenGettingEventByIdShouldRunQueryOnce(){
        when(eventRepository.findById(anyLong())).thenReturn(eventOptional);

        eventService.getEventById(anyLong());

        verify(eventRepository, times(1)).findById(anyLong());
    }
    @Test
    public void whenGettingEventByIdShouldThrowEvenNotFoundException(){
        when(eventRepository.findById(anyLong())).thenReturn(Optional.empty());

        EventNotFoundException eventNotFoundException = assertThrows(EventNotFoundException.class
                , () ->  eventService.getEventById(anyLong()));

        verify(eventRepository, times(1)).findById(anyLong());
    }
    @Test
    public void whenGettingEventByIdShouldMapItToEventDto(){
        when(eventRepository.findById(anyLong())).thenReturn(eventOptional);

        eventService.getEventById(anyLong());

        verify(eventMapper, times(1)).mapEventToEventWithUsersDto(any(Event.class));
    }

    @Disabled
    @Test
    public void whenGettingEventByIdShouldReturnEventWithUsersDto(){
        when(eventRepository.findById(anyLong())).thenReturn(eventOptional);

        var output = eventService.getEventById(anyLong());

        assertEquals(EventWithUsersDto.class,output.getClass());
    }




    /*
     ********************************************************************************************************************
     *                                       UPDATING EVENT TESTS
     ********************************************************************************************************************
     */
    @Test
    public void whenUpdatingEventShould(){

    }
}