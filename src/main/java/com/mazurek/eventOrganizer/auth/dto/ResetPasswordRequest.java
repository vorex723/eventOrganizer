package com.mazurek.eventOrganizer.auth.dto;

import com.mazurek.eventOrganizer.validators.ValidPassword;
import com.mazurek.eventOrganizer.validators.ValidationConstraints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Password must be provided.")
        @Size(min = ValidationConstraints.PASSWORD_MIN_LENGTH, max = ValidationConstraints.PASSWORD_MAX_LENGTH, message = "Password can not be shorter than 8 characters and longer than 32")
        @ValidPassword
        String password,
        @NotBlank(message = "Password confirmation must be provided.")
        @Size(min = ValidationConstraints.PASSWORD_MIN_LENGTH, max = ValidationConstraints.PASSWORD_MAX_LENGTH, message = "Password can not be shorter than 8 characters and longer than 32")
        @ValidPassword
        String passwordConfirmation
) {
}
