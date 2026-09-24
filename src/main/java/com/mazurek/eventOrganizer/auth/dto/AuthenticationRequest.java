package com.mazurek.eventOrganizer.auth.dto;

import com.mazurek.eventOrganizer.validators.ValidationConstraints;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {

    @NotBlank(message = "Email must be provided.")
    @Size(max = ValidationConstraints.EMAIL_MAX_LENGTH, message = "Email can not be longer than 255 characters.")
    @Email(regexp = ValidationConstraints.EMAIL_PATTERN, message = "Incorrect email address.")
    private String email;
    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = ValidationConstraints.PASSWORD_MIN_LENGTH, max = ValidationConstraints.PASSWORD_MAX_LENGTH, message = "Password can not be shorter than 8 characters and longer than 32")
    private String password;
}
