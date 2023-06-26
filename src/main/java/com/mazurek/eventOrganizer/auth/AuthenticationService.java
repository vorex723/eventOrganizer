package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.exception.InvalidEmailException;
import com.mazurek.eventOrganizer.exception.NotMatchingPasswordsException;
import com.mazurek.eventOrganizer.exception.UserAlreadyExistException;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CityUtils cityUtils;
    private final AuthenticationManager authenticationManager;


    public AuthenticationResponse register(RegisterRequest registerRequest) throws RuntimeException {
        if(userRepository.findByEmail(registerRequest.getEmail()).isPresent())
            throw new UserAlreadyExistException("Email is already used.");
        if(!registerRequest.getPassword().equals(registerRequest.getPasswordConfirmation()))
            throw new NotMatchingPasswordsException("Passwords are not matching.");
        if(registerRequest.getEmail().equals(registerRequest.getEmailConfirmation()))
            throw new InvalidEmailException("E-mails are not the same.");

        var user = User.builder()
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(Role.USER)
                .homeCity(cityUtils.resolveCity(registerRequest.getHomeCity()))
                .lastPasswordChangeTime(Calendar.getInstance().getTimeInMillis())
                //.lastPasswordChangeTime(System.currentTimeMillis())
                .build();

        userRepository.save(user);
        var jwtToken = jwtUtil.generateToken(user);
        return  AuthenticationResponse.builder().token(jwtToken).build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) throws AuthenticationException {

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(authenticationRequest.getEmail(), authenticationRequest.getPassword());

        authenticationManager.authenticate(authenticationToken);
        var user = userRepository.findByEmail(authenticationRequest.getEmail()).orElseThrow();
        var jwtToken = jwtUtil.generateToken(user);
        return  AuthenticationResponse.builder().token(jwtToken).build();
    }
}
