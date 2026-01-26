package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.user.User;

import java.util.UUID;

public interface AuthenticationService {
     void register(RegisterRequest registerRequest);
     AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest, DeviceType deviceType);
     AuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
     void logout(RefreshTokenRequest refreshTokenRequest);
     ActivationResult activateAccount(UUID token);
     void regenerateActivationTokenByUserEmail(String email);

     User getCurrentUser();
     UUID getCurrentUserId();
}
