package com.mazurek.eventOrganizer.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String emailConfirmation;
    private String homeCity;
    private String password;
    private String passwordConfirmation;
}
