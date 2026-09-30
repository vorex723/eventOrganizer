package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.common.EntityIdentity;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;
import java.util.Locale;


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
        this.name = name.toLowerCase(Locale.ROOT).trim();
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
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || EntityIdentity.persistentClass(this) != EntityIdentity.persistentClass(o)) {
            return false;
        }
        City other = (City) o;
        Object identifier = EntityIdentity.identifier(this, this::getId);
        return identifier != null && identifier.equals(EntityIdentity.identifier(other, other::getId));
    }

    @Override
    public final int hashCode() {
        return City.class.hashCode();
    }
}
