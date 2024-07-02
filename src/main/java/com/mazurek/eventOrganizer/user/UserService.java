package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.AuthenticationResponse;
import com.mazurek.eventOrganizer.user.dto.*;

import java.util.UUID;

public interface UserService {
    UserWithEventsDto getUserById(UUID id);
    AuthenticationResponse changeUserPassword(ChangeUserPasswordDto changeUserPasswordDto, String jwtToken);
    AuthenticationResponse changeUserEmail(ChangeUserEmailDto changeUserEmailDto, String jwtToken);
    UserWithEventsDto changeUserDetails(ChangeUserDetailsDto changeUserDetailsDto, String jwtToken);
    Boolean registerUserFcmToken(RegisterFcmTokenRequest registerFcmTokenRequest, String jwtToken);
}
