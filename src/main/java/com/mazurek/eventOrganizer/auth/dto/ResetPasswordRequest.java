package com.mazurek.eventOrganizer.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Password must be provided.")
        @Size(min = 8, max = 32, message = "Password can not be shorter than 8 characters and longer than 32")
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+!=])(?=\\S+$).{8,32}$",
                message = "Password must contain lowercase, uppercase, number and special sign"
        )
        String password,
        @NotBlank(message = "Password confirmation must be provided.")
        String passwordConfirmation
) {
}
