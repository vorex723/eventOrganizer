package com.mazurek.eventOrganizer.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserWithoutEventsDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;

}
