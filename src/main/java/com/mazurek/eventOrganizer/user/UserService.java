package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.AuthenticationResponse;
import com.mazurek.eventOrganizer.user.dto.ChangeUserDetailsDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserEmailDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserPasswordDto;
import com.mazurek.eventOrganizer.user.dto.UserWithEventsDto;

public interface UserService {
    UserWithEventsDto getUserById(Long id);
    AuthenticationResponse changeUserPassword(ChangeUserPasswordDto changeUserPasswordDto, String jwtToken);
    UserWithEventsDto changeUserEmail(ChangeUserEmailDto changeUserEmailDto, String jwtToken);
    UserWithEventsDto changeUserDetails(ChangeUserDetailsDto changeUserDetailsDto, String jwtToken);
}
