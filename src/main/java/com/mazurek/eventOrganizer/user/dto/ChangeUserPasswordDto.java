package com.mazurek.eventOrganizer.user.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeUserPasswordDto {
    private String newPassword;
    private String newPasswordConfirmation;
    private String password;
}
