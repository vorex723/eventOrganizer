package com.mazurek.eventOrganizer.auth.dto;

import com.mazurek.eventOrganizer.validators.ValidTimeZone;
import com.mazurek.eventOrganizer.validators.ValidPassword;
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
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "First name can not be shorter than 2 characters and longer than 20")
    @Size(min = 2, max = 20, message = "First name can not be shorter than 2 characters and longer than 20")
    private String firstName;

    @NotBlank(message = "Last name can not be shorter than 2 characters and longer than 20")
    @Size(min = 2, max = 20, message = "Last name can not be shorter than 2 characters and longer than 20")
    private String lastName;

    @NotBlank(message = "Email must be provided.")
    @Size(max = ValidationConstraints.EMAIL_MAX_LENGTH, message = "Email can not be longer than 255 characters.")
    @Email(regexp = ValidationConstraints.EMAIL_PATTERN, message = "Incorrect email address.")
    private String email;

    @NotBlank(message = "Email confirmation must be provided.")
    @Size(max = ValidationConstraints.EMAIL_MAX_LENGTH, message = "Email confirmation can not be longer than 255 characters.")
    @Email(regexp = ValidationConstraints.EMAIL_PATTERN, message = "Incorrect email address.")
    private String emailConfirmation;

    @NotBlank(message = "Home city can not be shorter than 3 characters and longer than 30")
    @Size(min = ValidationConstraints.CITY_MIN_LENGTH, max = ValidationConstraints.CITY_MAX_LENGTH, message = "Home city can not be shorter than 3 characters and longer than 30")
    private String homeCity;

    @NotBlank(message = "Time zone can not be shorter than 3 and longer than 35 characters")
    @Size(min = 3, max = 35, message = "Time zone can not be shorter than 3 and longer than 35 characters")
    @ValidTimeZone
    private String timeZone;

    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = ValidationConstraints.PASSWORD_MIN_LENGTH, max = ValidationConstraints.PASSWORD_MAX_LENGTH, message = "Password can not be shorter than 8 characters and longer than 32")
    @ValidPassword
    private String password;


    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = ValidationConstraints.PASSWORD_MIN_LENGTH, max = ValidationConstraints.PASSWORD_MAX_LENGTH, message = "Password can not be shorter than 8 characters and longer than 32")
    @ValidPassword
    private String passwordConfirmation;
}
