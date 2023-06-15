package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.exception.NotMatchingPasswordsException;
import com.mazurek.eventOrganizer.exception.UserAlreadyExistException;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;


    public AuthenticationResponse register(RegisterRequest registerRequest) throws RuntimeException {
        if(userRepository.findByEmail(registerRequest.getEmail()).isPresent())
            throw new UserAlreadyExistException("Email is already used.");
        if(!registerRequest.getPassword().equals(registerRequest.getPasswordConfirmation()))
            throw new NotMatchingPasswordsException("Passwords are not matching.");

        var user = User.builder()
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(Role.USER)
                .homeCity(resolveCity(registerRequest.getHomeCity()))
                .build();

        userRepository.save(user);
        var jwtToken = jwtUtil.generateToken(user);
        return  AuthenticationResponse.builder().token(jwtToken).build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) throws AuthenticationException {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authenticationRequest.getEmail(), authenticationRequest.getPassword()
                )
        );

        var user = userRepository.findByEmail(authenticationRequest.getEmail()).orElseThrow();
        var jwtToken = jwtUtil.generateToken(user);
        return  AuthenticationResponse.builder().token(jwtToken).build();
    }

    private City resolveCity(String cityName){
        /* city list needed for additional city name verifying */
        if (cityName == null || cityName.isBlank())
            return null;

        Optional<City> cityOptional = cityRepository.findByName(cityName);
        return cityOptional.orElseGet(() -> cityRepository.save(new City(cityName)));
   }
}
