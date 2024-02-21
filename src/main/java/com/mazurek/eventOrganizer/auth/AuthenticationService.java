package com.mazurek.eventOrganizer.auth;

import java.util.UUID;

public interface AuthenticationService {
     boolean register(RegisterRequest registerRequest);
     AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest);
     void activateAccount(UUID tokenId);
}
