package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.exception.auth.AccountAlreadyActivatedException;
import com.mazurek.eventOrganizer.exception.auth.ActivationTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.*;
import com.mazurek.eventOrganizer.notification.EmailService;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {


    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final CityService cityService;

    private final Long ACTIVATION_TOKEN_EXPIRATION_TIME_MILLISECONDS = 345600000L; //4DAYS

    @Transactional
    public void register(RegisterRequest registerRequest){
        if(userRepository.findByIgnoreCaseEmail(registerRequest.getEmail()).isPresent())
            throw new UserAlreadyExistException();
        if(!registerRequest.getPassword().equals(registerRequest.getPasswordConfirmation()))
            throw new NotMatchingPasswordsException();
        if(!registerRequest.getEmail().equalsIgnoreCase(registerRequest.getEmailConfirmation()))
            throw new NotMatchingEmailsException();

        Instant createDateTime = Instant.now();
        Role roleUser = roleRepository.findByName("ROLE_USER").orElseThrow(UserRoleNotFoundException::new);

        User user = User.builder()
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .email(registerRequest.getEmail().toLowerCase())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .homeCity(cityService.getCityByNameOrCreate(registerRequest.getHomeCity()))
                .createdAt(createDateTime)
                .timeZone(registerRequest.getTimeZone())
                .lastCredentialsChangeTime(createDateTime)
                .build();

        user.addRole(roleUser);
        User newUser = userRepository.save(user);

        ActivationToken activationToken = activationTokenRepository.save(ActivationToken.builder()
                .expirationDate(createDateTime.plusMillis(ACTIVATION_TOKEN_EXPIRATION_TIME_MILLISECONDS))
                .token(UUID.randomUUID())
                .user(newUser)
                .build());

        emailService.sendActivationEmail(newUser.getEmail(), activationToken.getToken());

    }

    @Transactional
    public ActivationResult activateAccount(UUID token){
        ActivationToken activationToken = activationTokenRepository.findByToken(token).orElseThrow(ActivationTokenNotFoundException::new);
        if (activationToken.isExpired()){
            activationToken.regenerate(ACTIVATION_TOKEN_EXPIRATION_TIME_MILLISECONDS);
            activationTokenRepository.save(activationToken);
            emailService.sendActivationEmail(activationToken.getUser().getEmail(), activationToken.getToken());
            return ActivationResult.TOKEN_EXPIRED_NEW_SENT;
        }
        User user = activationToken.getUser();
        user.setActivated(true);
        userRepository.save(user);
        activationTokenRepository.delete(activationToken);

        return ActivationResult.ACTIVATED;
    }

    @Transactional
    public void regenerateActivationTokenByUserEmail(String email){
        User user = userRepository.findByIgnoreCaseEmail(email).orElseThrow(UserNotFoundException::new);
        if (user.isActivated())
            throw new AccountAlreadyActivatedException();

        ActivationToken activationToken = activationTokenRepository.findByIgnoreCaseUserEmail(email)
                .orElseGet(() -> ActivationToken.builder()
                                    .user(user)
                                    .build());

        activationToken.regenerate(ACTIVATION_TOKEN_EXPIRATION_TIME_MILLISECONDS);
        activationTokenRepository.save(activationToken);

        emailService.sendActivationEmail(email, activationToken.getToken());
    }

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest, DeviceType deviceType) throws AuthenticationException {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequest.getEmail(), authenticationRequest.getPassword())
        );

        User user = userRepository.findByIgnoreCaseEmail(authenticationRequest.getEmail())
                .orElseThrow(() -> new IllegalStateException("User have to exist after authentication."));

        String accessToken = jwtUtils.generateAccessToken(user);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, deviceType);

        return  AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .accessTokenExpiration(jwtUtils.getAccessTokenExpiration())
                .build();
    }

    @Override
    @Transactional
    public AuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        RefreshToken refreshToken = refreshTokenService.verifyAndGetRefreshToken(refreshTokenRequest.refreshToken());

        User user = refreshToken.getUser();
        if (user.isBanned())
            throw new UserBannedException();

        String newAccessToken = jwtUtils.generateAccessToken(user);

        if(refreshToken.getDeviceType().shouldRotateRefreshToken()){
            refreshTokenService.revokeRefreshToken(refreshToken.getToken());
            RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user, refreshToken.getDeviceType());

            return new AuthenticationResponse(
                    newAccessToken,
                    newRefreshToken.getToken(),
                    jwtUtils.getAccessTokenExpiration());
        }

        return new AuthenticationResponse(
                newAccessToken,
                refreshToken.getToken(),
                jwtUtils.getAccessTokenExpiration());
    }

    @Override
    public void logout(RefreshTokenRequest refreshTokenRequest) {
        refreshTokenService.revokeRefreshToken(refreshTokenRequest.refreshToken());
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserDetails userDetails))
            throw new UserNotAuthenticatedException();

        User user = userRepository.findById(userDetails.getId()).orElseThrow(UserNotFoundException::new);
        if (user.isBanned())
            throw new UserBannedException();
        return user;
    }

    @Override
    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserDetails userDetails))
            throw new UserNotAuthenticatedException();

        return userDetails.getId();
    }




}
