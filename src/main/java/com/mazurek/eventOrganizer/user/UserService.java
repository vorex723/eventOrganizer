package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.dto.AuthenticationResponse;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.user.dto.*;

import java.util.UUID;

public interface UserService {
    UserProfileDto getUserById(UUID id);

    AuthenticationResponse changePassword(ChangeUserPasswordDto changeUserPasswordDto, DeviceType deviceType, String deviceInfo);
    AuthenticationResponse changeEmail(ChangeUserEmailDto changeUserEmailDto, DeviceType deviceType, String deviceInfo);
    UserProfileDto changeDetails(ChangeUserDetailsDto changeUserDetailsDto);
    Boolean registerUserFcmToken(RegisterFcmTokenRequest registerFcmTokenRequest);
    void banUser(UUID userId);
}
