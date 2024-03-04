package com.mazurek.eventOrganizer.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeUserEmailDto {
    @Email(regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")
    private String newEmail;
    @Email(regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")
    private String newEmailConfirmation;

    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = 8, max = 20, message = "Password can not be shorter than 8 characters and longer than 32")
    private String password;
}
