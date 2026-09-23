package com.mazurek.eventOrganizer.user.dto;

import com.mazurek.eventOrganizer.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CurrentUserDto {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String homeCity;
    private String timeZone;

    public CurrentUserDto(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.homeCity = user.getHomeCity().getName();
        this.timeZone = user.getTimeZone();
    }
}
