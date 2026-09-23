package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationResponse;
import com.mazurek.eventOrganizer.user.dto.*;

import java.util.UUID;

public interface UserService {
    UserProfileDto getUserById(UUID id);
    CurrentUserDto getCurrentUser();

    AuthenticationResponse changePassword(ChangeUserPasswordDto changeUserPasswordDto, DeviceType deviceType, String deviceInfo);
    void changeEmail(ChangeUserEmailDto changeUserEmailDto);
    CurrentUserDto changeDetails(ChangeUserDetailsDto changeUserDetailsDto);
    void banUser(UUID userId);
}
