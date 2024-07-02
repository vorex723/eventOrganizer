package com.mazurek.eventOrganizer.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class UserWithoutEventsDto {

    private UUID id;
    private String firstName;
    private String lastName;

}
