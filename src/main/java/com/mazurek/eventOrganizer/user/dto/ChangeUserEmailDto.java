package com.mazurek.eventOrganizer.user.dto;

import com.mazurek.eventOrganizer.validators.ValidationConstraints;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeUserEmailDto {
    @NotBlank(message = "New email must be provided.")
    @Size(max = ValidationConstraints.EMAIL_MAX_LENGTH, message = "New email can not be longer than 255 characters.")
    @Email(regexp = ValidationConstraints.EMAIL_PATTERN)
    private String newEmail;
    @NotBlank(message = "New email confirmation must be provided.")
    @Size(max = ValidationConstraints.EMAIL_MAX_LENGTH, message = "New email confirmation can not be longer than 255 characters.")
    @Email(regexp = ValidationConstraints.EMAIL_PATTERN)
    private String newEmailConfirmation;

    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = ValidationConstraints.PASSWORD_MIN_LENGTH, max = ValidationConstraints.PASSWORD_MAX_LENGTH, message = "Password can not be shorter than 8 characters and longer than 32")
    private String password;
}
