package com.mazurek.eventOrganizer.auth.dto;

import jakarta.validation.constraints.NotBlank;
import com.mazurek.eventOrganizer.validators.ValidPassword;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Password must be provided.")
        @Size(min = 8, max = 32, message = "Password can not be shorter than 8 characters and longer than 32")
        @ValidPassword
        String password,
        @NotBlank(message = "Password confirmation must be provided.")
        @Size(min = 8, max = 32, message = "Password can not be shorter than 8 characters and longer than 32")
        @ValidPassword
        String passwordConfirmation
) {
}
