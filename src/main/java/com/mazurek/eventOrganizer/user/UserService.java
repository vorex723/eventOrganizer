package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.AuthenticationResponse;
import com.mazurek.eventOrganizer.event.dto.EventOverviewDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.user.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserProfileDto getUserById(UUID id);

    AuthenticationResponse changePassword(ChangeUserPasswordDto changeUserPasswordDto, DeviceType deviceType, String deviceInfo);
    AuthenticationResponse changeEmail(ChangeUserEmailDto changeUserEmailDto, DeviceType deviceType, String deviceInfo);
    UserWithEventsDto changeDetails(ChangeUserDetailsDto changeUserDetailsDto);
    Boolean registerUserFcmToken(RegisterFcmTokenRequest registerFcmTokenRequest);
    void banUser(UUID userId);
    void logoutFromAllDevices();
    void logoutFromAllDevices(UUID userId);
}
