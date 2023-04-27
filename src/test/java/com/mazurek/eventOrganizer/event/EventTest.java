package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class EventTest {


    @Test
    void settingOwnerShouldSetProperOwnerAndAddEventToHisOwnEvents()
    {
        User owner = new User();
        Event event = new Event();

        event.setOwner(owner);
        assertEquals(true, event.getOwner().equals(owner));
        assertEquals(true, owner.getUserEvents().contains(event));
    }
    @Test
    void changingOwnerShouldChangeOwnerAndRemoveEventFromOriginalOwnerEvents(){
        User firstOwner = new User();
        User secondOwner = new User();
        Event event = new Event();

        event.setOwner(firstOwner);
        event.setOwner(secondOwner);

        assertEquals(true, event.getOwner().equals(secondOwner));
        assertEquals(true, secondOwner.getUserEvents().contains(event));
        assertEquals(false, firstOwner.getUserEvents().contains(event));

    }
    @Test
    void addingAttenderShouldAddEventToUserAttendingEvents(){
        User attender = new User();
        Event event = new Event();

        event.addAttendingUser(attender);

        assertEquals(true, event.getAttendingUsers().contains(attender));
        assertEquals(true, attender.getAttendingEvents().contains(event));
    }

    @Test
    void removingAttenderShouldRemoveEventFromUserAttendingEvents(){
        User attender = new User();
        Event event = new Event();

        event.addAttendingUser(attender);
        event.removeAttendingUser(attender);

        assertEquals(false, event.getAttendingUsers().contains(attender));
        assertEquals(false, attender.getAttendingEvents().contains(event));

    }

    @Test
    void addingTagToEventShouldAddEventToTagEventList(){
        Event event = new Event();
        Tag newTag = new Tag();

        event.addTag(newTag);

        assertEquals(true, event.getTags().contains(newTag));
        assertEquals(true, newTag.getEvents().contains(event));
    }

    @Test
    void removingTagShouldRemoveEventFromTagEventList(){
        Event event = new Event();
        Tag tag = new Tag();

        event.addTag(tag);
        event.removeTag(tag);

        assertEquals(false, event.getTags().contains(tag));
        assertEquals(false, tag.getEvents().contains(event));
    }
    @Test
    void whenCityIsNotGivenShouldSetCityAsNull(){
        Event event = new Event();

        event.setCity(null);

        assertEquals(null,event.getCity());
    }

    @Test
    void settingCityShouldAddEventToCityEventList(){
        Event event = new Event();
        City city = new City();

        event.setCity(city);

        assertEquals(true, event.getCity().equals(city));
        assertEquals(true, city.getEvents().contains(event));
    }

    @Test
    void changingCityShouldRemoveEventFromOldCity(){
        Event event = new Event();
        City oldCity = new City();
        City newCity = new City();

        event.setCity(oldCity);
        event.setCity(newCity);

        assertEquals(false, event.getCity().equals(oldCity));
        assertEquals(false, oldCity.getEvents().contains(event));
        assertEquals(true, event.getCity().equals(newCity));
        assertEquals(true, newCity.getEvents().contains(event));

    }
}
