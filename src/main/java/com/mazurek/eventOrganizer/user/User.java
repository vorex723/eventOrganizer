package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.common.EntityIdentity;
import com.mazurek.eventOrganizer.city.City;
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
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    private Instant lastCredentialsChangeTime;

    @Builder.Default
    @Column(nullable = false)
    private long securityVersion = 0;

    @Builder.Default
    @Column(nullable = false)
    private long notificationPreferencesVersion = 0;

    @Builder.Default
    private boolean activated = false;

    @Builder.Default
    private boolean banned = false;

    public User() {
    }

    public void addRole(Role role){
        this.roles.add(role);
    }

    public String getFullName(){
        return firstName + " " + lastName;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || EntityIdentity.persistentClass(this) != EntityIdentity.persistentClass(o)) {
            return false;
        }
        User other = (User) o;
        Object identifier = EntityIdentity.identifier(this, this::getId);
        return identifier != null && identifier.equals(EntityIdentity.identifier(other, other::getId));
    }

    @Override
    public final int hashCode() {
        return User.class.hashCode();
    }
}
