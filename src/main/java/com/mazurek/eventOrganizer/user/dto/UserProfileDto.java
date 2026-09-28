package com.mazurek.eventOrganizer.user.dto;

import com.mazurek.eventOrganizer.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    private static final String DELETED_USER_FIRST_NAME = "Deleted";
    private static final String DELETED_USER_LAST_NAME = "user";

    private UUID id;
    private String firstName;
    private String lastName;
    private String homeCity;

    public UserProfileDto(User user) {
        if (user == null) {
            this.firstName = DELETED_USER_FIRST_NAME;
            this.lastName = DELETED_USER_LAST_NAME;
            return;
        }
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.homeCity = user.getHomeCity().getName();
    }

    public static UserProfileDto deletedUser() {
        return new UserProfileDto(null, DELETED_USER_FIRST_NAME, DELETED_USER_LAST_NAME, null);
    }

}
