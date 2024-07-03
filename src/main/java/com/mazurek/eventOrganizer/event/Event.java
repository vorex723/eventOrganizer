package com.mazurek.eventOrganizer.event;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.tag.Tag;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "events")
@ToString
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private String shortDescription;

    @Lob()
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

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "event_user", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> attendingUsers = new HashSet<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(name = "event_tag", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Thread> threads = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private Set<File> files = new HashSet<>();

    public String getIdAsString(){
        return id.toString();
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

    public boolean isUserAttending(User user){
        return this.attendingUsers.contains(user) || this.owner.equals(user);
    }

    public void addThread(Thread thread){
        if(this.threads.contains(thread))
            return;
        this.threads.add(thread);
        thread.setEvent(this);
    }

    public void addFile(File file){
        this.files.add(file);
    }
    public void removeFile(File file){
        this.files.remove(file);
    }

    public boolean hadPlace(){
        return eventStartDate.getTime() < Calendar.getInstance().getTimeInMillis();
    }

    public List<String> getAttendersFcmTokenList(){
        List<String> attendersFcmTokenList = new ArrayList<>();
        attendingUsers.forEach(user -> attendersFcmTokenList.add(user.getFcmAndroidToken()));
        attendersFcmTokenList.add(owner.getFcmAndroidToken());
        return attendersFcmTokenList;
    }
}