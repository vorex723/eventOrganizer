package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.user.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface AuthenticationService {
     void register(RegisterRequest registerRequest);
     AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest, DeviceType deviceType);
     AuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
     ActivationResult activateAccount(UUID token);
     void regenerateActivationTokenByUserEmail(String email);
     void logout(RefreshTokenRequest refreshTokenRequest);
     void logoutFromAllDevices();
     void logoutFromAllDevices(UUID userId);

     User getCurrentUser();
     UUID getCurrentUserId();
}
