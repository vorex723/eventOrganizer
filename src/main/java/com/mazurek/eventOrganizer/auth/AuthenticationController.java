package com.mazurek.eventOrganizer.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.mazurek.eventOrganizer.exception.auth.AccountAlreadyActivatedException;
import com.mazurek.eventOrganizer.exception.auth.VerificationTokenExpiredException;
import com.mazurek.eventOrganizer.exception.auth.VerificationTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.user.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.UUID;

@Controller
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationServiceImpl authenticationService;


    @PostMapping("/register")
    public ResponseEntity<?> registerNewUser(@Valid @RequestBody RegisterRequest registerRequest){
        try{
            if (authenticationService.register(registerRequest))
                return ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("Message", "Verify your email to get access."));
            else
                return ResponseEntity.internalServerError().build();

        } catch (UserAlreadyExistException | InvalidEmailException | NotMatchingPasswordsException exception){
            return ResponseEntity.badRequest().body(Collections.singletonMap("Message",exception.getMessage()));
        }
    }
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthenticationRequest authenticationRequest){
        try {
            return ResponseEntity.ok(authenticationService.authenticate(authenticationRequest));
        } catch (UserNotFoundException | InvalidPasswordException | AuthenticationException exception){
            return  ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("Message","Wrong email or password."));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> generateNewVerificationToken(@RequestBody EmailBasedRequest request){
        try {
            authenticationService.generateNewVerificationTokenByUserEmail(request.getEmail());
            return ResponseEntity.noContent().build();
        } catch (AccountAlreadyActivatedException exception){
            return ResponseEntity.badRequest().body(Collections.singletonMap("Message", exception.getMessage()));
        }

    }

    @GetMapping("/verify/{tokenId}")
    public ResponseEntity<?> verifyEmail(@PathVariable(name = "tokenId")UUID tokenId){
        try {
            authenticationService.activateAccount(tokenId);
            return ResponseEntity.ok().build();
        } catch (VerificationTokenNotFoundException exception){
            return ResponseEntity.notFound().build();
        } catch (VerificationTokenExpiredException exception){
            return ResponseEntity.badRequest().body(Collections.singletonMap("Message", exception.getMessage()));
        } catch (RuntimeException exception){
            return ResponseEntity.internalServerError().build();
        }

    }

}
