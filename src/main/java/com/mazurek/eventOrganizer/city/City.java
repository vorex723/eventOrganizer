package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "cities")
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
    }

}
