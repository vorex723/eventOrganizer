package com.mazurek.eventOrganizer.user.dto;

import com.mazurek.eventOrganizer.validators.ValidTimeZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangeUserDetailsDto {

    @NotBlank(message = "First name can not be shorter than 2 characters and longer than 20")
    @Size(min = 2, max = 20, message = "First name can not be shorter than 2 characters and longer than 20")
    private String firstName;
    @NotBlank(message = "Last name can not be shorter than 2 characters and longer than 20")
    @Size(min = 2, max = 20, message = "Last name can not be shorter than 2 characters and longer than 20")
    private String lastName;
    @NotBlank(message = "Home city can not be shorter than 3 characters and longer than 20")
    @Size(min = 3, max = 20, message = "Home city can not be shorter than 3 characters and longer than 20")
    private String homeCity;

    @NotBlank(message = "Time zone must be provided.")
    @Size(min = 3, max = 35, message = "Time zone cannot be shorter than 3 and longer than 35 characters.")
    @ValidTimeZone
    private String timeZone;

}
