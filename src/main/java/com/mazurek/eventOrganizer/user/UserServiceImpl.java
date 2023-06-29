package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.AuthenticationResponse;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.exception.*;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.user.dto.ChangeUserDetailsDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserEmailDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserPasswordDto;
import com.mazurek.eventOrganizer.user.dto.UserWithEventsDto;
import com.mazurek.eventOrganizer.user.mapper.UserMapper;
import lombok.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Calendar;

@Getter
@Setter
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService;
    private final UserMapper userMapper;
    private final CityUtils cityUtils;


    @Override
    public UserWithEventsDto getUserById(Long id) {
        return userMapper.userToUserWithEventsDto(userRepository.findById(id).orElseThrow(UserNotFoundException::new));
    }

    @Override
    public AuthenticationResponse changeUserPassword(ChangeUserPasswordDto changeUserPasswordDto,
            String jwtToken)
            throws RuntimeException
    {
        User user = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get();

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
            throw new UserAlreadyExistException("There already is account using this email.");

        User user = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get();

        if (!passwordEncoder.matches(changeUserEmailDto.getPassword(),user.getPassword()))
            throw new InvalidPasswordException("Wrong password.");

        user.setEmail(changeUserEmailDto.getNewEmail());
        user.setLastCredentialsChangeTime(Calendar.getInstance().getTimeInMillis());

        return  AuthenticationResponse.builder().token(jwtUtil.generateToken( userRepository.save(user))).build();
    }

    @Override
    public UserWithEventsDto changeUserDetails(ChangeUserDetailsDto changeUserDetailsDto, String jwtToken) {

        User user = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get();

        user.setFirstName(changeUserDetailsDto.getFirstName());
        user.setLastName(changeUserDetailsDto.getLastName());

        user.setHomeCity(cityUtils.resolveCity(changeUserDetailsDto.getHomeCity()));

        return userMapper.userToUserWithEventsDto(userRepository.save(user));
    }
}
