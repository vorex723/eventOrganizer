package com.mazurek.eventOrganizer.auth;

public interface AuthenticationService {
     AuthenticationResponse register(RegisterRequest registerRequest);
     AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest);
}
