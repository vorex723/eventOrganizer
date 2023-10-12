package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.exception.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;

@Controller
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationServiceImpl authenticationService;

    @PostMapping("/register")
    public ResponseEntity<?> registerNewUser(@RequestBody RegisterRequest registerRequest){
        try{
            return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.register(registerRequest));
        } catch (UserAlreadyExistException | InvalidEmailException | NotMatchingPasswordsException exception){
            return ResponseEntity.ok().body(Collections.singletonMap("Message",exception.getMessage()));
        }
    }
    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticateUser(@RequestBody AuthenticationRequest authenticationRequest){
        try {
            return ResponseEntity.ok(authenticationService.authenticate(authenticationRequest));
        } catch (UserNotFoundException | InvalidPasswordException | AuthenticationException exception){
            return  ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("Message","Wrong email or password"));
        }
    }

}
