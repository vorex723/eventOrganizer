package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;


@Getter
@Setter
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "cities",
        uniqueConstraints = @UniqueConstraint(columnNames = "name")
)
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    @Builder.Default
    @OneToMany(mappedBy = "city")
    private Set<Event> events = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "homeCity")
    private Set<User> residents = new HashSet<>();
    public City() {
        events = new HashSet<>();
        residents = new HashSet<>();
    }

    public City(String name) {
        this.name = name.toLowerCase();
        events = new HashSet<>();
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        City city = (City) o;
        return Objects.equals(id, city.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
