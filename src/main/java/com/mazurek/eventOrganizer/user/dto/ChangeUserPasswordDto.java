package com.mazurek.eventOrganizer.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeUserPasswordDto {
    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = 8, max = 20, message = "Password can not be shorter than 8 characters and longer than 32")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,32}$" ,
            message = "Password have to contain at least one lowercase character, one uppercase character, one number and one special sign")
    private String newPassword;

    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = 8, max = 20, message = "Password can not be shorter than 8 characters and longer than 32")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,32}$" ,
            message = "Password have to contain at least one lowercase character, one uppercase character, one number and one special sign")
    private String newPasswordConfirmation;

    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = 8, max = 20, message = "Password can not be shorter than 8 characters and longer than 32")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,32}$" ,
            message = "Password have to contain at least one lowercase character, one uppercase character, one number and one special sign")
    private String password;
}
