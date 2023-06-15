package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "cities")
@AllArgsConstructor
@Builder
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @OneToMany(mappedBy = "city", cascade = CascadeType.ALL)
    private List<Event> events;

    @OneToMany(mappedBy = "homeCity", cascade = CascadeType.ALL)
    private Set<User> residents;
    public City() {
        events = new ArrayList<>();
        residents = new HashSet<>();
    }

    public City(String name) {
        this.name = name;
        events = new ArrayList<>();
        residents = new HashSet<>();
    }

    public void addEvent(Event event){
        events.add(event);
    }
    public void removeEvent(Event event){
        events.remove(event);
    }

    public void addResident(User user){
        if (residents.contains(user))
            return;
        residents.add(user);
    }

    public void removeResident(User user){
        if (!residents.contains(user))
            return;
        residents.remove(user);
    }
}
