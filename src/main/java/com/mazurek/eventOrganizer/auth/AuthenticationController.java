package com.mazurek.eventOrganizer.auth;


import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.utils.DeviceTypeResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.UUID;

@Controller
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationServiceImpl authenticationService;
    private final DeviceTypeResolver deviceTypeResolver;

    @PostMapping("/register")
    public ResponseEntity<?> registerNewUser(@Valid @RequestBody RegisterRequest registerRequest){
        authenticationService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("Message", "Verify your email to get access."));

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
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        authenticationService.logout(refreshTokenRequest);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authenticationService.refreshToken(refreshTokenRequest));
    }

    @PostMapping("/activate")
    public ResponseEntity<?> generateNewActivationToken(@Valid @RequestBody EmailBasedRequest request){
            authenticationService.regenerateActivationTokenByUserEmail(request.getEmail());
            return ResponseEntity.ok().build();
    }

    @GetMapping("/activate/{tokenId}")
    public ResponseEntity<?> activateAccount(@PathVariable(name = "tokenId")UUID tokenId){
        authenticationService.activateAccount(tokenId);
        return ResponseEntity.ok().build();
    }
}
