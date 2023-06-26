package com.mazurek.eventOrganizer.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeUserDetailsDto {

    private String firstName;
    private String lastName;
    private String homeCity;

}
