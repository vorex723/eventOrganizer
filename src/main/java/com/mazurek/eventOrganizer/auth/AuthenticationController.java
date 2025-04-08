package com.mazurek.eventOrganizer.auth;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Controller
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationServiceImpl authenticationService;

    @PostMapping("/register")
    public ResponseEntity<?> registerNewUser(@Valid @RequestBody RegisterRequest registerRequest){
        authenticationService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("Message", "Verify your email to get access."));

    }
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticateUser(@Valid @RequestBody AuthenticationRequest authenticationRequest){
        return ResponseEntity.ok(authenticationService.authenticate(authenticationRequest));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> generateNewVerificationToken(@Valid @RequestBody EmailBasedRequest request){
            authenticationService.generateNewVerificationTokenByUserEmail(request.getEmail());
            return ResponseEntity.ok().build();
    }

    @GetMapping("/verify/{tokenId}")
    public ResponseEntity<?> verifyEmail(@PathVariable(name = "tokenId")UUID tokenId){
        authenticationService.activateAccount(tokenId);
        return ResponseEntity.ok().build();
    }
}
