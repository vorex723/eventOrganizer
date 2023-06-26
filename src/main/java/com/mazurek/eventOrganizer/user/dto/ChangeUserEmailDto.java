package com.mazurek.eventOrganizer.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeUserEmailDto {
    private String newEmail;
    private String newEmailConfirmation;
    private String password;
}
