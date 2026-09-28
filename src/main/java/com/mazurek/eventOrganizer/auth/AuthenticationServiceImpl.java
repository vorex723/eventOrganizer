package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationResponse;
import com.mazurek.eventOrganizer.auth.dto.RefreshTokenRequest;
import com.mazurek.eventOrganizer.auth.dto.RegisterRequest;
import com.mazurek.eventOrganizer.auth.dto.ResetPasswordRequest;
import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import com.mazurek.eventOrganizer.exception.auth.AccountAlreadyActivatedException;
import com.mazurek.eventOrganizer.exception.auth.ActivationTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.auth.PasswordResetTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.*;
import com.mazurek.eventOrganizer.notification.service.EmailService;
import com.mazurek.eventOrganizer.auth.email.AuthEmailType;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.user.AccountSessionInvalidationService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final AccountSessionInvalidationService accountSessionInvalidationService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final CityService cityService;
    private final AuthProperties authProperties;
    private final Clock clock;

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

        ActivationToken activationToken = ActivationToken.builder()
                .user(newUser)
                .build();
        activationToken.issue(
                UUID.randomUUID(),
                authProperties.getActivationTokenExpiration(),
                createDateTime
        );
        activationTokenRepository.save(activationToken);

        emailService.sendActivationEmail(newUser.getEmail(), activationToken.getToken());

    }

    @Transactional
    public ActivationResult activateAccount(UUID token){
        ActivationToken activationToken = activationTokenRepository.findByToken(token).orElseThrow(ActivationTokenNotFoundException::new);
        Instant now = clock.instant();
        if (activationToken.isExpired(now)){
            emailService.cancelPendingEmails(
                    activationToken.getUser().getId(),
                    AuthEmailType.ACCOUNT_ACTIVATION
            );
            activationToken.regenerate(authProperties.getActivationTokenExpiration(), now);
            activationTokenRepository.save(activationToken);
            emailService.sendActivationEmail(activationToken.getUser().getEmail(), activationToken.getToken());
            return ActivationResult.TOKEN_EXPIRED_NEW_SENT;
        }
        User user = activationToken.getUser();
        user.setActivated(true);
        userRepository.save(user);
        activationTokenRepository.delete(activationToken);
        emailService.cancelPendingEmails(user.getId(), AuthEmailType.ACCOUNT_ACTIVATION);

        return ActivationResult.ACTIVATED;
    }

    @Transactional
    public void regenerateActivationTokenByUserEmail(String email){
        Optional<User> candidate = userRepository.findByIgnoreCaseEmail(email)
                .filter(user -> !user.isActivated());
        if (candidate.isEmpty()) {
            return;
        }
        User user = candidate.get();

        if (emailService.wasRecentlyRequested(user.getId(), AuthEmailType.ACCOUNT_ACTIVATION)) {
            return;
        }

        ActivationToken activationToken = activationTokenRepository.findByIgnoreCaseUserEmail(email)
                .orElseGet(() -> ActivationToken.builder().user(user).build());

        emailService.cancelPendingEmails(user.getId(), AuthEmailType.ACCOUNT_ACTIVATION);
        activationToken.regenerate(authProperties.getActivationTokenExpiration(), clock.instant());
        activationTokenRepository.save(activationToken);

        emailService.sendActivationEmail(email, activationToken.getToken());
    }

    @Transactional
    @Override
    public void requestPasswordReset(String email) {
        userRepository.findByIgnoreCaseEmail(email)
                .filter(User::isActivated)
                .ifPresent(user -> {
                    if (emailService.wasRecentlyRequested(user.getId(), AuthEmailType.PASSWORD_RESET)) {
                        return;
                    }

                    PasswordResetToken token = passwordResetTokenRepository.findByUserId(user.getId())
                            .orElseGet(() -> PasswordResetToken.builder().user(user).build());
                    emailService.cancelPendingEmails(user.getId(), AuthEmailType.PASSWORD_RESET);
                    token.issue(
                            UUID.randomUUID(),
                            authProperties.getPasswordResetTokenExpiration(),
                            clock.instant()
                    );
                    passwordResetTokenRepository.save(token);
                    emailService.sendPasswordResetEmail(user.getEmail(), token.getToken());
                });
    }

    @Transactional
    @Override
    public void resetPassword(UUID token, ResetPasswordRequest request) {
        if (!request.password().equals(request.passwordConfirmation())) {
            throw new NotMatchingPasswordsException();
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(PasswordResetTokenNotFoundException::new);
        if (resetToken.isExpired(clock.instant())) {
            passwordResetTokenRepository.delete(resetToken);
            emailService.cancelPendingEmails(resetToken.getUser().getId(), AuthEmailType.PASSWORD_RESET);
            throw new PasswordResetTokenNotFoundException();
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setLastCredentialsChangeTime(clock.instant());
        accountSessionInvalidationService.invalidateAll(user);
        passwordResetTokenRepository.delete(resetToken);
        emailService.cancelPendingEmails(user.getId(), AuthEmailType.PASSWORD_RESET);
    }

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest, DeviceType deviceType) throws AuthenticationException {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequest.getEmail(), authenticationRequest.getPassword())
        );

        User user = userRepository.findByIgnoreCaseEmail(authenticationRequest.getEmail())
                .orElseThrow(() -> new IllegalStateException("User have to exist after authentication."));

        IssuedRefreshToken refreshToken = refreshTokenService.issueRefreshToken(user, deviceType);
        String accessToken = jwtUtils.generateAccessToken(user);

        return  AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.rawToken())
                .accessTokenExpiration(jwtUtils.getAccessTokenExpiration())
                .build();
    }

    @Override
    @Transactional
    public AuthenticationResponse refreshAccessToken(RefreshTokenRequest refreshTokenRequest) {
        RefreshTokenUse refreshTokenUse = refreshTokenService.useRefreshToken(refreshTokenRequest.refreshToken());
        User user = refreshTokenUse.refreshToken().getUser();
        if (user.isBanned()) {
            throw new UserBannedException();
        }

        String newAccessToken = jwtUtils.generateAccessToken(user);

        return new AuthenticationResponse(
                newAccessToken,
                refreshTokenUse.rawToken(),
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
        accountSessionInvalidationService.invalidateAll(getCurrentUser());
    }
    @Override
    @Transactional
    public void logoutFromAllDevices(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        accountSessionInvalidationService.invalidateAll(user);
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
