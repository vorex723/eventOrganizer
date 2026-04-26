package com.mazurek.eventOrganizer.auth;


import com.mazurek.eventOrganizer.auth.dto.*;
import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import com.mazurek.eventOrganizer.exception.auth.ActivationTokenNotFoundException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.utils.DeviceTypeResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final DeviceTypeResolver deviceTypeResolver;
    private final AuthProperties authProperties;

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> registerNewUser(@Valid @RequestBody RegisterRequest registerRequest){
        authenticationService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(RegistrationResponse.verificationRequired());

    }
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticateUser(@Valid @RequestBody AuthenticationRequest authenticationRequest,
                                                                   @RequestHeader(value = "User-Agent", required = false) String userAgent,
                                                                   @RequestHeader(value = "X-Device-Type", required = false) String deviceTypeHeader
    ){
        DeviceType deviceType = deviceTypeResolver.determineDeviceType(deviceTypeHeader, userAgent);
        return ResponseEntity.ok(authenticationService.authenticate(authenticationRequest, deviceType));
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        authenticationService.logout(refreshTokenRequest);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authenticationService.refreshAccessToken(refreshTokenRequest));
    }

    @PostMapping("/activate")
    public ResponseEntity<Void> generateNewActivationToken(@Valid @RequestBody EmailBasedRequest request){
            authenticationService.regenerateActivationTokenByUserEmail(request.getEmail());
            return ResponseEntity.noContent().build();
    }

    @GetMapping("/activate/{tokenId}")
    public ResponseEntity<Void> activateAccount(@PathVariable(name = "tokenId")UUID tokenId){
        try {
            ActivationResult activationResult = authenticationService.activateAccount(tokenId);
            return redirectToActivationResult(activationResult);
        } catch (ActivationTokenNotFoundException exception) {
            return redirectToActivationResult(ActivationResult.INVALID_TOKEN);
        }
    }

    private ResponseEntity<Void> redirectToActivationResult(ActivationResult activationResult) {
        URI redirectUri = UriComponentsBuilder.fromUriString(authProperties.getActivationResultBaseUrl())
                .queryParam("status", activationResult.getRedirectStatus())
                .build(true)
                .toUri();
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .location(redirectUri)
                .build();
    }
}
