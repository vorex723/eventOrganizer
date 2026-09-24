package com.mazurek.eventOrganizer.user.dto;

import com.mazurek.eventOrganizer.validators.ValidPassword;
import com.mazurek.eventOrganizer.validators.ValidationConstraints;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeUserPasswordDto {
    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = ValidationConstraints.PASSWORD_MIN_LENGTH, max = ValidationConstraints.PASSWORD_MAX_LENGTH, message = "Password can not be shorter than 8 characters and longer than 32")
    @ValidPassword
    private String newPassword;

    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = ValidationConstraints.PASSWORD_MIN_LENGTH, max = ValidationConstraints.PASSWORD_MAX_LENGTH, message = "Password can not be shorter than 8 characters and longer than 32")
    @ValidPassword
    private String newPasswordConfirmation;

    @NotBlank(message = "Password can not be shorter than 8 characters and longer than 32")
    @Size(min = ValidationConstraints.PASSWORD_MIN_LENGTH, max = ValidationConstraints.PASSWORD_MAX_LENGTH, message = "Password can not be shorter than 8 characters and longer than 32")
    private String password;
}
