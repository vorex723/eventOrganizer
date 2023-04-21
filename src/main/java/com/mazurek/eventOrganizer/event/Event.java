package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "events")
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
    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;
    private String exactAddress;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User owner;
    @ManyToMany
    @JoinTable(name = "event_user", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> attendingUsers;
    @ManyToMany
    @JoinTable(name = "event_tag", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags;


    public Event() {
       attendingUsers = new HashSet<>();
       tags = new HashSet<>();
    }
    public void setOwner(User user){
        if(owner != null && !owner.equals(user)) {
            owner.removeUserEvent(this);
        }
        owner = user;
        owner.addUserEvent(this);
    }
    public void addAttendingUser(User user){
        if(attendingUsers.contains(user))
            return;
        attendingUsers.add(user);
        user.addAttendingEvent(this);
    }
    public void removeAttendingUser(User user){
        if(!attendingUsers.contains(user))
            return;
        attendingUsers.remove(user);
        user.removeAttendingEvent(this);
    }
    public void addTag(Tag tag){
        if(tags.contains(tag))
            return;
        tags.add(tag);
        tag.addEvent(this);
    }
    public void removeTag(Tag tag){
       if(!tags.contains(tag))
           return;
       tags.remove(tag);
       tag.removeEvent(this);
    }

    public void setCity(City newCity){
        if(newCity == null){
            city = null;
            return;
        }
        if(city != null) {
            if (city.equals(newCity))
                return;
            if(city.getEvents().contains(this))
                city.removeEvent(this);
        }
        city = newCity;
        city.addEvent(this);

    }

}
