package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String shortDescription;
    private String longDescription;
    private LocalDateTime createDate;
    private LocalDateTime lastUpdate;
    private LocalDateTime eventStartDate;
    private City city;
    private String exactAddress;
    private User owner;
    private Set<User> attendingUsers;


    public Event() {
        attendingUsers = new HashSet<>();
    }
}
