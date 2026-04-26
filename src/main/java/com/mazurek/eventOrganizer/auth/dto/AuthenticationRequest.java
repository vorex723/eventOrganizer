package com.mazurek.eventOrganizer.auth.dto;

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
    @Size(max = 255, message = "Email can not be longer than 255 characters.")
    @Email(regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$", message = "Incorrect email address.")
    private String email;
    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = 8, max = 32, message = "Password can not be shorter than 8 characters and longer than 32")
    private String password;
}
