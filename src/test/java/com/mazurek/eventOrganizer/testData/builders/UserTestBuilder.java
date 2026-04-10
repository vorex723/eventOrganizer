package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

/**
 * Builder for creating User test objects with sensible defaults.
 * Use this to create users in tests instead of manual construction.
 */
public class UserTestBuilder {

    private UUID id = UserConstants.FIRST_USER_ID;
    private String firstName = UserConstants.FIRST_USER_FIRST_NAME;
    private String lastName = UserConstants.FIRST_USER_LAST_NAME;
    private String email = UserConstants.FIRST_USER_EMAIL;
    private String password = UserConstants.USER_PASSWORD;
    private String timeZone = UserConstants.FIRST_USER_TIMEZONE;
    private City homeCity = CityTestBuilder.warsaw().build();
    private Instant createdAt = TimeConstants.NOW;
    private Instant lastCredentialsChangeTime = TimeConstants.NOW;
    private Set<Role> roles = new HashSet<>(Set.of(RoleTestBuilder.userRole().build()));
    private boolean activated = true;
    private boolean banned = false;
    private String fcmAndroidToken;


    public static UserTestBuilder firstUser() {
        return new UserTestBuilder()
                .id(UserConstants.FIRST_USER_ID)
                .firstName(UserConstants.FIRST_USER_FIRST_NAME)
                .lastName(UserConstants.FIRST_USER_LAST_NAME)
                .email(UserConstants.FIRST_USER_EMAIL);
    }

    public static UserTestBuilder secondUser() {
        return new UserTestBuilder()
                .id(UserConstants.SECOND_USER_ID)
                .firstName(UserConstants.SECOND_USER_FIRST_NAME)
                .lastName(UserConstants.SECOND_USER_LAST_NAME)
                .email(UserConstants.SECOND_USER_EMAIL);
    }

    public static UserTestBuilder thirdUser() {
        return new UserTestBuilder()
                .id(UserConstants.THIRD_USER_ID)
                .firstName(UserConstants.THIRD_USER_FIRST_NAME)
                .lastName(UserConstants.THIRD_USER_LAST_NAME)
                .email(UserConstants.THIRD_USER_EMAIL);
    }

    public static UserTestBuilder adminUser() {
        return new UserTestBuilder()
                .id(UserConstants.ADMIN_USER_ID)
                .firstName(UserConstants.ADMIN_USER_FIRST_NAME)
                .lastName(UserConstants.ADMIN_USER_LAST_NAME)
                .email(UserConstants.ADMIN_USER_EMAIL);
    }

    public static UserTestBuilder deletedUser() {
        return new UserTestBuilder()
                .id(UserConstants.DELETED_USER_ID)
                .firstName(UserConstants.DELETED_USER_FIRST_NAME)
                .lastName(UserConstants.DELETED_USER_LAST_NAME)
                .email(UserConstants.DELETED_USER_EMAIL)
                .activated(false)
                .banned(true);
    }


    public UserTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public UserTestBuilder firstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public UserTestBuilder lastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public UserTestBuilder email(String email) {
        this.email = email;
        return this;
    }

    public UserTestBuilder password(String password) {
        this.password = password;
        return this;
    }

    public UserTestBuilder homeCity(City homeCity) {
        this.homeCity = homeCity;
        return this;
    }

    public UserTestBuilder timeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public UserTestBuilder createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public UserTestBuilder lastCredentialsChangeTime(Instant lastCredentialsChangeTime) {
        this.lastCredentialsChangeTime = lastCredentialsChangeTime;
        return this;
    }

    public UserTestBuilder roles(Set<Role> roles) {
        this.roles = new HashSet<>(roles);
        return this;
    }

    public UserTestBuilder addRole(Role role) {
        this.roles.add(role);
        return this;
    }

    public UserTestBuilder activated(boolean activated) {
        this.activated = activated;
        return this;
    }

    public UserTestBuilder banned(boolean banned) {
        this.banned = banned;
        return this;
    }

    public UserTestBuilder fcmAndroidToken(String fcmAndroidToken) {
        this.fcmAndroidToken = fcmAndroidToken;
        return this;
    }

    public User build() {
        User user = User.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(password)
                .homeCity(homeCity)
                .timeZone(timeZone)
                .createdAt(createdAt)
                .lastCredentialsChangeTime(lastCredentialsChangeTime)
                .roles(new HashSet<>(roles))
                .activated(activated)
                .banned(banned)
                .fcmAndroidToken(fcmAndroidToken)
                .build();

        return user;
    }
}
