package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.AuthenticationResponse;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.jwt.JwtUtils;
import com.mazurek.eventOrganizer.jwt.RefreshToken;
import com.mazurek.eventOrganizer.jwt.RefreshTokenService;
import com.mazurek.eventOrganizer.user.dto.*;
import lombok.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtils jwtUtils;
    private final CityService cityService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final int PAGE_DEFAULT_SIZE = 30;


    @Override
    @Transactional
    public UserProfileDto getUserById(UUID id) {
        return new UserProfileDto(userRepository.findById(id).orElseThrow(UserNotFoundException::new));
    }

    @Override
    @Transactional
    public UserWithEventsDto changeDetails(ChangeUserDetailsDto changeUserDetailsDto) {

        User user = authenticationService.getCurrentUser();
        user.setFirstName(changeUserDetailsDto.getFirstName());
        user.setLastName(changeUserDetailsDto.getLastName());
        user.setHomeCity(cityService.getCityByNameOrCreate(changeUserDetailsDto.getHomeCity()));

        return new UserWithEventsDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public AuthenticationResponse changePassword(ChangeUserPasswordDto changeUserPasswordDto,
                                                     DeviceType deviceType,
                                                     String deviceInfo
                                                     ) throws RuntimeException
    {
        User user = authenticationService.getCurrentUser();

        if (!passwordEncoder.matches(changeUserPasswordDto.getPassword(),user.getPassword()))
            throw new InvalidPasswordException("Old password is not matching.");
        if (!changeUserPasswordDto.getNewPassword().equals(changeUserPasswordDto.getNewPasswordConfirmation()))
            throw new NotMatchingPasswordsException();

        user.setPassword(passwordEncoder.encode(changeUserPasswordDto.getNewPassword()));
        user.setLastCredentialsChangeTime(Instant.now());
        userRepository.save(user);

        refreshTokenService.revokeAllUserTokens(user.getId());

        String newAccessToken = jwtUtils.generateAccessToken(user);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(
                user,
                deviceType
        );

        return new AuthenticationResponse(
                newAccessToken,
                newRefreshToken.getToken(),
                jwtUtils.getAccessTokenExpiration()
        );

    }

    @Override
    @Transactional
    public AuthenticationResponse changeEmail(
            ChangeUserEmailDto changeUserEmailDto,
            DeviceType deviceType,
            String deviceInfo
           )
    {
        User user = authenticationService.getCurrentUser();

        if (!passwordEncoder.matches(changeUserEmailDto.getPassword(), user.getPassword()))
            throw new InvalidPasswordException();
        if (user.getEmail().equalsIgnoreCase(changeUserEmailDto.getNewEmail()))
            throw new SameEmailException();
        if (!changeUserEmailDto.getNewEmail().equalsIgnoreCase(changeUserEmailDto.getNewEmailConfirmation()))
            throw new NotMatchingEmailsException();
        if (userRepository.findByEmail(changeUserEmailDto.getNewEmail()).isPresent())
            throw new UserAlreadyExistException();

        user.setEmail(changeUserEmailDto.getNewEmail().toLowerCase());
        user.setLastCredentialsChangeTime(Instant.now());

        userRepository.save(user);

        refreshTokenService.revokeAllUserTokens(user.getId());

        String newAccessToken = jwtUtils.generateAccessToken(user);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(
                user,
                deviceType
        );

        return new AuthenticationResponse(
                newAccessToken,
                newRefreshToken.getToken(),
                jwtUtils.getAccessTokenExpiration()
        );
    }

    @Override
    @Transactional
    public Boolean registerUserFcmToken(RegisterFcmTokenRequest registerFcmTokenRequest) {
        try{
            User user = authenticationService.getCurrentUser();
            user.setFcmAndroidToken(registerFcmTokenRequest.getToken());
            userRepository.save(user);
            return true;
        } catch (Exception e) {
           return false;
        }
    }

    @Override
    public void banUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        user.setBanned(true);
        userRepository.save(user);
        refreshTokenService.revokeAllUserTokens(userId);
    }

    @Override
    public void logoutFromAllDevices() {
        refreshTokenService.revokeAllUserTokens(authenticationService.getCurrentUserId());
    }
    @Override
    public void logoutFromAllDevices(UUID userId) {
        refreshTokenService.revokeAllUserTokens(userId);
    }
}
