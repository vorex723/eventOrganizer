package com.mazurek.eventOrganizer.auth.dto;

import com.mazurek.eventOrganizer.validators.ValidationConstraints;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailBasedRequest
{
    @NotBlank(message = "Email must be provided.")
    @Size(max = ValidationConstraints.EMAIL_MAX_LENGTH, message = "Email can not be longer than 255 characters.")
    @Email(regexp = ValidationConstraints.EMAIL_PATTERN, message = "Incorrect email address.")
    String email;
}
