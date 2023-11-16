package com.mazurek.eventOrganizer.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    @Email(regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$", message = "Incorrect email address.")
    private String email;

    @Email(regexp = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$", message = "Incorrect email address.")
    private String emailConfirmation;

    @NotBlank(message = "Home city can not be shorter than 3 characters and longer than 20")
    @Size(min = 3, max = 20, message = "Home city can not be shorter than 3 characters and longer than 20")
    private String homeCity;


    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = 8, max = 20, message = "Password can not be shorter than 8 characters and longer than 32")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,32}$",
            message = "Password have to contain at least one lowercase character, one uppercase character, one number and one special sign")
    private String password;


    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = 8, max = 20, message = "Password can not be shorter than 8 characters and longer than 32")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,32}$" ,
            message = "Password have to contain at least one lowercase character, one uppercase character, one number and one special sign")
    private String passwordConfirmation;
}
