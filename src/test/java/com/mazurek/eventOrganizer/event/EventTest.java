package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class EventTest {
    private User firstOwner;
    private User secondOwner;
    private Event event;
    private City cityRzeszow;
    private City cityKrakow;
    private Tag tagSpring;

    private BCryptPasswordEncoder passwordEncoder = Mockito.spy(new BCryptPasswordEncoder());
    private final String EVENT_OWNER_FIRST_NAME = "Andrew";
    private final String EVENT_OWNER_LAST_NAME = "Golota";
    private final String PASSWORD_DEFAULT = "password";
    private final String EVENT_SHORT_DESCRIPTION = "short description should be short";
    private final String EVENT_LONG_DESCRIPTION = "long description can be quite long, and it Should be. maybe i should put Lorem Ipsum here.";
    private final String EVENT_NAME = "First Event";
    private final String EVENT_EXACT_ADDRESS = "ul. Dąbrowskiego 3";
    @BeforeEach
    void setUp(){
        firstOwner = User.builder()
                .id(1L)
                .email("example@dot.com")
                .role(Role.USER)
                .firstName(EVENT_OWNER_FIRST_NAME)
                .lastName(EVENT_OWNER_LAST_NAME)
                .attendingEvents(new ArrayList<>())
                .userEvents(new ArrayList<>())
                .lastCredentialsChangeTime(Calendar.getInstance().getTimeInMillis())
                .build();

        secondOwner = User.builder()
                .id(2L)
                .role(Role.USER)
                .firstName("andrzej")
                .lastName("wesoly")
                .email("notExample@dot.com")
                .userEvents(new ArrayList<>())
                .attendingEvents(new ArrayList<>())
                .build();

        event = Event.builder()
                .id(1L)
                .name(EVENT_NAME)
                .shortDescription(EVENT_SHORT_DESCRIPTION)
                .longDescription(EVENT_LONG_DESCRIPTION)
                .createDate(new Date(Calendar.getInstance().getTimeInMillis()))
                .tags(new HashSet<>())
                .attendingUsers(new HashSet<>())
                .city(cityRzeszow)
                .exactAddress(EVENT_EXACT_ADDRESS)
                .build();

        cityRzeszow = City.builder()
                .id(1L)
                .name("Rzeszow")
                .events(new ArrayList<>())
                .residents(new HashSet<>())
                .build();

        cityKrakow = City.builder()
                .id(2L)
                .name("Krakow")
                .events(new ArrayList<>())
                .residents(new HashSet<>())
                .build();

        tagSpring = Tag.builder()
                .name("spring")
                .id(2L)
                .events(new HashSet<>())
                .build();
    }

    @Test
    void settingOwnerShouldSetProperOwnerAndAddEventToHisOwnEvents()
    {

        event.setOwner(firstOwner);
        assertEquals(true, event.getOwner().equals(firstOwner));
        assertEquals(true, firstOwner.getUserEvents().contains(event));
    }
    @Test
    void changingOwnerShouldChangeOwnerAndRemoveEventFromOriginalOwnerEvents(){

        event.setOwner(firstOwner);
        event.setOwner(secondOwner);

        assertEquals(true, event.getOwner().equals(secondOwner));
        assertEquals(true, secondOwner.getUserEvents().contains(event));
        assertEquals(false, firstOwner.getUserEvents().contains(event));

    }
    @Test
    void addingAttenderShouldAddEventToUserAttendingEvents(){

        event.addAttendingUser(secondOwner);

        assertEquals(true, event.getAttendingUsers().contains(secondOwner));
        assertEquals(true, secondOwner.getAttendingEvents().contains(event));
    }

    @Test
    void removingAttenderShouldRemoveEventFromUserAttendingEvents(){


        event.addAttendingUser(secondOwner);
        event.removeAttendingUser(secondOwner);

        assertEquals(false, event.getAttendingUsers().contains(secondOwner));
        assertEquals(false, secondOwner.getAttendingEvents().contains(event));

    }

    @Test
    void addingTagToEventShouldAddEventToTagEventList(){

        event.addTag(tagSpring);

        assertEquals(true, event.getTags().contains(tagSpring));
        assertEquals(true, tagSpring.getEvents().contains(event));
    }

    @Test
    void removingTagShouldRemoveEventFromTagEventList(){

        event.addTag(tagSpring);
        event.removeTag(tagSpring);

        assertEquals(false, event.getTags().contains(tagSpring));
        assertEquals(false, tagSpring.getEvents().contains(event));
    }
    @Test
    void whenCityIsNotGivenShouldSetCityAsNull(){

        event.setCity(null);

        assertEquals(null,event.getCity());
    }

    @Test
    void settingCityShouldAddEventToCityEventList(){

        event.setCity(cityRzeszow);

        assertEquals(true, event.getCity().equals(cityRzeszow));
        assertEquals(true, cityRzeszow.getEvents().contains(event));
    }

    @Test
    void changingCityShouldRemoveEventFromOldCity(){

        event.setCity(cityRzeszow);
        event.setCity(cityKrakow);

        assertEquals(false, event.getCity().equals(cityRzeszow));
        assertEquals(false, cityRzeszow.getEvents().contains(event));
        assertEquals(true, event.getCity().equals(cityKrakow));
        assertEquals(true, cityKrakow.getEvents().contains(event));

    }
}
