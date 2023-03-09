package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String email;
    private City homeCity;
    private String encodedPassword;

    private List<Event> userEvents;

    private List<Event> attendingEvents;

    public User() {
        userEvents = new ArrayList<>();
        attendingEvents = new ArrayList<>();
    }
}
