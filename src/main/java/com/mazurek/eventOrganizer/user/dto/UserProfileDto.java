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

    private UUID id;
    private String firstName;
    private String lastName;
    private String homeCity;

    public UserProfileDto(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.homeCity = user.getHomeCity().getName();
    }

}
