package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.event.Event;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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
        if (!email.equals(user.email)) return false;
        return Objects.equals(homeCity, user.homeCity);
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
