package com.mazurek.eventOrganizer.user.dto;

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
public class RegisterFcmTokenRequest {
    @NotBlank(message = "FCM token must be provided.")
    @Size(max = 4096, message = "FCM token can not be longer than 4096 characters.")
    String token;
}
