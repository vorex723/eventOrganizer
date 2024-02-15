package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import com.mazurek.eventOrganizer.file.File;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.thread.ThreadReply;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Entity
@Getter
@Setter
@Table(name = "users")
@Builder
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;
    private String email;
    @ManyToOne
    @JoinColumn(name = "city_id")
    private City homeCity;
    private String password;
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Event> userEvents;
    @ManyToMany(mappedBy = "attendingUsers", cascade = CascadeType.ALL)
    private List<Event> attendingEvents;
    @Enumerated(EnumType.STRING)
    private Role role;
    @OneToMany(mappedBy = "owner", cascade = CascadeType.PERSIST,  fetch = FetchType.LAZY)
    private Set<Thread> threads = new HashSet<>();
    @OneToMany(mappedBy = "replier", cascade = CascadeType.PERSIST,  fetch = FetchType.LAZY)
    private Set<ThreadReply> threadReplies = new HashSet<>();
    @OneToMany(mappedBy = "owner", cascade = CascadeType.PERSIST)
    private Set<File> files = new HashSet<>();

    private Long lastCredentialsChangeTime;

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

    public void setHomeCity(City newHomeCity){
        if(homeCity != null)
        {
            if (homeCity.equals(newHomeCity))
                return;
            homeCity.removeResident(this);
        }
        homeCity = newHomeCity;
        if(homeCity != null)
            homeCity.addResident(this);
    }

    public void removeThread(Thread thread){
        this.threads.remove(thread);
    }

    public void addThread(Thread thread){
        this.threads.add(thread);
    }

    public void addFile(File file){
        this.files.add(file);
    }
    public void removeFile(File file){
        this.files.remove(file);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (!id.equals(user.id)) return false;
        if (!firstName.equals(user.firstName)) return false;
        if (!lastName.equals(user.lastName)) return false;
        return email.equals(user.email);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + firstName.hashCode();
        result = 31 * result + lastName.hashCode();
        result = 31 * result + email.hashCode();
        return result;
    }
}
