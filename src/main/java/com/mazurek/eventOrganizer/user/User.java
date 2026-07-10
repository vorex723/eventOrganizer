package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.threadReply.ThreadReply;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.*;

@Entity
@Getter
@Setter
@Table(name = "users")
@Builder
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @ManyToOne
    @JoinColumn(name = "city_id", nullable = false)
    private City homeCity;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private String timeZone;

    @Builder.Default
    @OneToMany(mappedBy = "owner")
    private Set<Event> userEvents = new HashSet<>();

    @Builder.Default
    @ManyToMany(mappedBy = "attendingUsers")
    private Set<Event> attendingEvents = new HashSet<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    private Set<Thread> threads = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "replier", fetch = FetchType.LAZY)
    private Set<ThreadReply> threadReplies = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "owner")
    private Set<File> files = new HashSet<>();

    @Column(nullable = false)
    private Instant lastCredentialsChangeTime;

    @Builder.Default
    private boolean activated = false;

    @Builder.Default
    private boolean banned = false;

    public User() {
        userEvents = new HashSet<>();
        attendingEvents = new HashSet<>();
        threads = new HashSet<>();
        threadReplies = new HashSet<>();
        files = new HashSet<>();
    }

    public void addRole(Role role){
        this.roles.add(role);
    }

    public void removeRole(Role role){
        this.roles.remove(role);
    }

    public void addAttendingEvent(Event event){
        if(attendingEvents.contains(event))
            return;
        attendingEvents.add(event);
        event.getAttendingUsers().add(this);
    }

    public void removeAttendingEvent(Event event){
        if(!attendingEvents.contains(event))
            return;
        attendingEvents.remove(event);
        event.getAttendingUsers().remove(this);
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

    public void setHomeCity(City newHomeCity) {
        if (this.homeCity == newHomeCity)
            return;

        if (this.homeCity != null) {
            this.homeCity.removeResident(this);
        }

        this.homeCity = newHomeCity;

        if (newHomeCity != null) {
            newHomeCity.addResident(this);
        }
    }

    public void removeThread(Thread thread){
        this.threads.remove(thread);
    }

    public void addThread(Thread thread){
        this.threads.add(thread);
    }

    public void addThreadReply(ThreadReply threadReply){
        this.threadReplies.add(threadReply);
    }

    public void removeThreadReply(ThreadReply threadReply){
        this.threadReplies.remove(threadReply);
    }

    public void addFile(File file){
        this.files.add(file);
    }

    public void removeFile(File file){
        this.files.remove(file);
    }

    public String getFullName(){
        return firstName + " " + lastName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
