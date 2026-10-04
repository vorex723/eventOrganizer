package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.common.EntityIdentity;
import com.mazurek.eventOrganizer.city.City;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
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

    @Size(max = 255)
    @NotBlank
    @Column(nullable = false, length = 255)
    private String firstName;

    @Size(max = 255)
    @NotBlank
    @Column(nullable = false, length = 255)
    private String lastName;

    @Size(max = 255)
    @NotBlank
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "city_id", nullable = false)
    private City homeCity;

    @Size(max = 255)
    @NotBlank
    @Column(nullable = false, length = 255)
    private String password;

    @NotNull
    @Column(nullable = false)
    private Instant createdAt;

    @Size(max = 255)
    @NotBlank
    @Column(nullable = false, length = 255)
    private String timeZone;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "role_id", nullable = false)
    )
    private Set<Role> roles = new HashSet<>();

    @NotNull
    @Column(nullable = false)
    private Instant lastCredentialsChangeTime;

    @PositiveOrZero
    @Builder.Default
    @Column(nullable = false)
    private long securityVersion = 0;

    @PositiveOrZero
    @Builder.Default
    @Column(nullable = false)
    private long notificationPreferencesVersion = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean activated = false;

    @Builder.Default
    @Column(nullable = false)
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
