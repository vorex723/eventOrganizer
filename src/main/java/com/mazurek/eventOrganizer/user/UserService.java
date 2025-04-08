package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.AuthenticationResponse;
import com.mazurek.eventOrganizer.event.dto.EventOverviewDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;
import com.mazurek.eventOrganizer.user.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserProfileDto getUserById(UUID id);

    AuthenticationResponse changeUserPassword(ChangeUserPasswordDto changeUserPasswordDto, String jwtToken);
    AuthenticationResponse changeUserEmail(ChangeUserEmailDto changeUserEmailDto, String jwtToken);
    UserWithEventsDto changeUserDetails(ChangeUserDetailsDto changeUserDetailsDto, String jwtToken);
    Boolean registerUserFcmToken(RegisterFcmTokenRequest registerFcmTokenRequest, String jwtToken);
}
