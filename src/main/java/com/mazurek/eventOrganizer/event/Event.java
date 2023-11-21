package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@Table(name = "events")
@ToString
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String shortDescription;
    private String longDescription;
    private Date createDate;
    private Date lastUpdate;
    private Date eventStartDate;
    @ManyToOne
    @JoinColumn(name = "city_id")
    private City city;
    private String exactAddress;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User owner;
    @ManyToMany
    @JoinTable(name = "event_user", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> attendingUsers = new HashSet<>();
    @ManyToMany
    @JoinTable(name = "event_tag", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Thread> threads = new HashSet<>();

    public Event() {
     //  attendingUsers = new HashSet<>();
      // tags = new HashSet<>();
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

    public void clearTags(){
        for (Tag tag : tags){
            tag.removeEvent(this);
        }
        tags.clear();
    }

    public boolean containsTagByName(String tagName){
        for (Tag tag : this.tags){
            if(tag.getName().equals(tagName))
                return true;
        }
        return false;
    }

}
