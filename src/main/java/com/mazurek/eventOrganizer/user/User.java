package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String email;
    @ManyToOne
    @JoinColumn(name = "city_id")
    private City homeCity;
    private String encodedPassword;
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Event> userEvents;
    @ManyToMany(mappedBy = "attendingUsers", cascade = CascadeType.ALL)
    private List<Event> attendingEvents;

    public User() {
        userEvents = new ArrayList<>();
        attendingEvents = new ArrayList<>();
    }

    public void addAttendingEvent(Event event){
        if(attendingEvents.contains(event))
            return;
        attendingEvents.add(event);
    }
    public void removeAttendingEvent(Event event){
        if(!attendingEvents.contains(event))
            return;
        attendingEvents.remove(event);
    }

    public void addUserEvent(Event event){
        if(userEvents.contains(event))
            return;
        userEvents.add(event);
    }

    public void removeUserEvent(Event event){
        if(!userEvents.contains(event))
            return;
        userEvents.remove(event);
    }
}
