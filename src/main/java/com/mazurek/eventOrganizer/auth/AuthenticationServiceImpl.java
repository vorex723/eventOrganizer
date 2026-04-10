package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationResponse;
import com.mazurek.eventOrganizer.auth.dto.RefreshTokenRequest;
import com.mazurek.eventOrganizer.auth.dto.RegisterRequest;
import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.config.properties.AuthProperties;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Service
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
    private final AuthProperties authProperties;
    private final Clock clock;

    @Autowired
    public AuthenticationServiceImpl(UserRepository userRepository,
                                     RoleRepository roleRepository,
                                     ActivationTokenRepository activationTokenRepository,
                                     RefreshTokenService refreshTokenService,
                                     EmailService emailService,
                                     AuthenticationManager authenticationManager,
                                     PasswordEncoder passwordEncoder,
                                     JwtUtils jwtUtils,
                                     CityService cityService,
                                     AuthProperties authProperties,
                                     Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.cityService = cityService;
        this.authProperties = authProperties;
        this.clock = clock;
    }

    public AuthenticationServiceImpl(UserRepository userRepository,
                                     RoleRepository roleRepository,
                                     ActivationTokenRepository activationTokenRepository,
                                     RefreshTokenService refreshTokenService,
                                     EmailService emailService,
                                     AuthenticationManager authenticationManager,
                                     PasswordEncoder passwordEncoder,
                                     JwtUtils jwtUtils,
                                     CityService cityService) {
        this(
                userRepository,
                roleRepository,
                activationTokenRepository,
                refreshTokenService,
                emailService,
                authenticationManager,
                passwordEncoder,
                jwtUtils,
                cityService,
                defaultAuthProperties(),
                Clock.systemUTC());
    }

    public AuthenticationServiceImpl(UserRepository userRepository,
                                     RoleRepository roleRepository,
                                     ActivationTokenRepository activationTokenRepository,
                                     RefreshTokenService refreshTokenService,
                                     EmailService emailService,
                                     AuthenticationManager authenticationManager,
                                     PasswordEncoder passwordEncoder,
                                     JwtUtils jwtUtils,
                                     CityService cityService,
                                     Clock clock) {
        this(
                userRepository,
                roleRepository,
                activationTokenRepository,
                refreshTokenService,
                emailService,
                authenticationManager,
                passwordEncoder,
                jwtUtils,
                cityService,
                defaultAuthProperties(),
                clock);
    }

    private static AuthProperties defaultAuthProperties() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setActivationTokenExpiration(345600000L);
        authProperties.setActivationResultBaseUrl("http://localhost:3000/activation-result");
        return authProperties;
    }

    @Transactional
    public void register(RegisterRequest registerRequest){
        if(userRepository.findByIgnoreCaseEmail(registerRequest.getEmail()).isPresent())
            throw new UserAlreadyExistException();
        if(!registerRequest.getPassword().equals(registerRequest.getPasswordConfirmation()))
            throw new NotMatchingPasswordsException();
        if(!registerRequest.getEmail().equalsIgnoreCase(registerRequest.getEmailConfirmation()))
            throw new NotMatchingEmailsException();

        Instant createDateTime = clock.instant();
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
                .expirationDate(createDateTime.plusMillis(authProperties.getActivationTokenExpiration()))
                .token(UUID.randomUUID())
                .user(newUser)
                .build());

        emailService.sendActivationEmail(newUser.getEmail(), activationToken.getToken());

    }

    @Transactional
    public ActivationResult activateAccount(UUID token){
        ActivationToken activationToken = activationTokenRepository.findByToken(token).orElseThrow(ActivationTokenNotFoundException::new);
        Instant now = clock.instant();
        if (activationToken.isExpired(now)){
            activationToken.regenerate(authProperties.getActivationTokenExpiration(), now);
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

        activationToken.regenerate(authProperties.getActivationTokenExpiration(), clock.instant());
        activationTokenRepository.save(activationToken);

        emailService.sendActivationEmail(email, activationToken.getToken());
    }

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest, DeviceType deviceType) throws AuthenticationException {

        authenticationManager.authenticate(
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
    public AuthenticationResponse refreshAccessToken(RefreshTokenRequest refreshTokenRequest) {
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
    @Transactional
    public void logout(RefreshTokenRequest refreshTokenRequest) {
        refreshTokenService.revokeRefreshToken(refreshTokenRequest.refreshToken());
    }

    @Override
    @Transactional
    public void logoutFromAllDevices() {
        refreshTokenService.revokeAllUserTokens(this.getCurrentUserId());
    }
    @Override
    @Transactional
    public void logoutFromAllDevices(UUID userId) {
        refreshTokenService.revokeAllUserTokens(userId);
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof JwtUserDetails userDetails))
            throw new UserNotAuthenticatedException();

        User user = userRepository.findById(userDetails.getId()).orElseThrow(UserNotFoundException::new);
        if (user.isBanned())
            throw new UserBannedException();
        return user;
    }

    @Override
    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof JwtUserDetails userDetails))
            throw new UserNotAuthenticatedException();

        return userDetails.getId();
    }




}
