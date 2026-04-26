package com.mazurek.eventOrganizer.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeUserEmailDto {
    @NotBlank(message = "New email must be provided.")
    @Size(max = 255, message = "New email can not be longer than 255 characters.")
    @Email(regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")
    private String newEmail;
    @NotBlank(message = "New email confirmation must be provided.")
    @Size(max = 255, message = "New email confirmation can not be longer than 255 characters.")
    @Email(regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")
    private String newEmailConfirmation;

    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = 8, max = 32, message = "Password can not be shorter than 8 characters and longer than 32")
    private String password;
}
