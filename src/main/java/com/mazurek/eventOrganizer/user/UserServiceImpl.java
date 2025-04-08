package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.AuthenticationResponse;
import com.mazurek.eventOrganizer.auth.AuthenticationServiceImpl;
import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.event.dto.EventOverviewDto;
import com.mazurek.eventOrganizer.event.dto.EventOverviewPageDto;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.user.dto.*;
import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;

    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationServiceImpl authenticationService;
    private final CityUtils cityUtils;
    private final int PAGE_DEFAULT_SIZE = 30;


    @Override
    @Transactional
    public UserProfileDto getUserById(UUID id) {
        return new UserProfileDto(userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found.")));
    }

    @Override
    @Transactional
    public UserWithEventsDto changeUserDetails(ChangeUserDetailsDto changeUserDetailsDto, String jwtToken) {

        User user = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).orElseThrow(() -> new UserNotFoundException("User not found."));
        user.setFirstName(changeUserDetailsDto.getFirstName());
        user.setLastName(changeUserDetailsDto.getLastName());
        user.setHomeCity(cityUtils.resolveCity(changeUserDetailsDto.getHomeCity()));

        return new UserWithEventsDto(userRepository.save(user));
    }

    @Override
    public AuthenticationResponse changeUserPassword(ChangeUserPasswordDto changeUserPasswordDto,
                                                     String jwtToken) throws RuntimeException
    {
        User user = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).orElseThrow(() -> new UserNotFoundException("User not found."));

        if (!passwordEncoder.matches(changeUserPasswordDto.getPassword(),user.getPassword()))
            throw new InvalidPasswordException("Old password is not matching.");
        if (!changeUserPasswordDto.getNewPassword().equals(changeUserPasswordDto.getNewPasswordConfirmation()))
            throw new NotMatchingPasswordsException("Passwords are not matching.");

        user.setPassword(passwordEncoder.encode(changeUserPasswordDto.getNewPassword()));
        user.setLastCredentialsChangeTime(System.currentTimeMillis());
        return AuthenticationResponse.builder().token(jwtUtil.generateToken(userRepository.save(user))).build();
    }

    @Override
    public AuthenticationResponse changeUserEmail(
            ChangeUserEmailDto changeUserEmailDto,
            String jwtToken)
    {
        if (!changeUserEmailDto.getNewEmail().equals(changeUserEmailDto.getNewEmailConfirmation()))
            throw new InvalidEmailException("Emails are not the same");
        if (userRepository.findByEmail(changeUserEmailDto.getNewEmail()).isPresent())
            throw new UserAlreadyExistException("There is account using this email.");


        User user = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).orElseThrow(() -> new UserNotFoundException("User not found."));

        if (!passwordEncoder.matches(changeUserEmailDto.getPassword(),user.getPassword()))
            throw new InvalidPasswordException("Wrong password.");

        user.setEmail(changeUserEmailDto.getNewEmail());
        user.setLastCredentialsChangeTime(Calendar.getInstance().getTimeInMillis());

        return  AuthenticationResponse.builder().token(jwtUtil.generateToken(userRepository.save(user))).build();
    }

    @Override
    public Boolean registerUserFcmToken(RegisterFcmTokenRequest registerFcmTokenRequest, String jwtToken) {
        try{
            User user = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).orElseThrow(() -> new UserNotFoundException("User not found."));
            user.setFcmAndroidToken(registerFcmTokenRequest.getToken());
            userRepository.save(user);
            return true;
        } catch (Exception e) {
           return false;
        }
    }

}
