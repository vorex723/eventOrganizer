package com.mazurek.eventOrganizer.user.dto;

import jakarta.validation.constraints.NotBlank;
import com.mazurek.eventOrganizer.validators.ValidPassword;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeUserPasswordDto {
    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = 8, max = 32, message = "Password can not be shorter than 8 characters and longer than 32")
    @ValidPassword
    private String newPassword;

    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = 8, max = 32, message = "Password can not be shorter than 8 characters and longer than 32")
    @ValidPassword
    private String newPasswordConfirmation;

    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = 8, max = 32, message = "Password can not be shorter than 8 characters and longer than 32")
    private String password;
}
