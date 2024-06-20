package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.exception.auth.AccountAlreadyActivatedException;
import com.mazurek.eventOrganizer.exception.auth.VerificationTokenExpiredException;
import com.mazurek.eventOrganizer.exception.auth.VerificationTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    public static final String VERIFICATION_URL = "localhost:8080/api/v1/auth/verify/";
    private static final String ACTIVATION_EMAIL_BODY = "You can activate your account by opening this link: ";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CityUtils cityUtils;
    private final AuthenticationManager authenticationManager;
    private final VerificationTokenRepository verificationTokenRepository;
    private final JavaMailSender javaMailSender;
    private final long VERIFICATION_TOKEN_EXPIRATION_TIME = 172800000;


    public boolean register(RegisterRequest registerRequest) throws RuntimeException {
        if(userRepository.findByEmail(registerRequest.getEmail()).isPresent())
            throw new UserAlreadyExistException("Email is already used.");
        if(!registerRequest.getPassword().equals(registerRequest.getPasswordConfirmation()))
            throw new NotMatchingPasswordsException("Passwords are not matching.");
        if(!registerRequest.getEmail().equals(registerRequest.getEmailConfirmation()))
            throw new InvalidEmailException("E-mails are not matching.");

        User user = User.builder()
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .role(Role.USER)
                .homeCity(cityUtils.resolveCity(registerRequest.getHomeCity()))
                .lastCredentialsChangeTime(Calendar.getInstance().getTimeInMillis())
                .build();
        user = userRepository.save(user);

        VerificationToken verificationToken = verificationTokenRepository.save(VerificationToken.builder()
                .expirationDate(new Date(Calendar.getInstance().getTimeInMillis() + VERIFICATION_TOKEN_EXPIRATION_TIME))
                .user(user)
                .build());

        sendVerificationEmail(user.getEmail(), verificationToken.getId());

        return user.getId() != null;
    }

    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) throws RuntimeException {

        User user = userRepository.findByEmail(authenticationRequest.getEmail()).orElseThrow(UserNotFoundException::new);
        if (!passwordEncoder.matches(authenticationRequest.getPassword(), user.getPassword()))
            throw new InvalidPasswordException("Wrong password.");

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(authenticationRequest.getEmail(), authenticationRequest.getPassword());

        authenticationManager.authenticate(authenticationToken);

        String jwtToken = jwtUtil.generateToken(user);

        return  AuthenticationResponse.builder().token(jwtToken).build();
    }

    public void activateAccount(UUID tokenId){
        VerificationToken token = verificationTokenRepository.findById(tokenId).orElseThrow(() -> new VerificationTokenNotFoundException("Token does not exist!"));
        if (token.isExpired()){
            verificationTokenRepository.delete(token);
            throw new VerificationTokenExpiredException("Your verification token has expired.");
        }
        token.getUser().setActivated(true);
        userRepository.save(token.getUser());
        verificationTokenRepository.delete(token);
    }

    public void generateNewVerificationTokenByOldTokenId(UUID oldTokenId){
        VerificationToken oldToken = verificationTokenRepository.findById(oldTokenId).orElseThrow(() -> new VerificationTokenNotFoundException("This token does not exist."));
        VerificationToken newToken = verificationTokenRepository.save(
                VerificationToken.builder()
                        .user(oldToken.getUser())
                        .expirationDate(new Date(Calendar.getInstance().getTimeInMillis()+VERIFICATION_TOKEN_EXPIRATION_TIME))
                        .build());

        sendVerificationEmail(oldToken.getUser().getEmail(), newToken.getId());

        verificationTokenRepository.delete(oldToken);
    }
    public void generateNewVerificationTokenByUserEmail(String email){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new  UserNotFoundException("There is no such user."));
        if (user.isEnabled())
            throw new AccountAlreadyActivatedException("This account was already activated.");

        Optional<VerificationToken> oldToken = verificationTokenRepository.findByUserEmail(email);
        oldToken.ifPresent(verificationTokenRepository::delete);
        VerificationToken newToken = verificationTokenRepository.save(VerificationToken.builder()
                        .user(user)
                        .expirationDate(new Date(Calendar.getInstance().getTimeInMillis()+VERIFICATION_TOKEN_EXPIRATION_TIME))
                        .build());
        sendVerificationEmail(email, newToken.getId());
    }

    private void sendVerificationEmail(String userEmail, UUID tokenID) {
        String body =   ACTIVATION_EMAIL_BODY + VERIFICATION_URL + tokenID;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEmail);
        message.setFrom("no-reply@eventorganizer.cba.pl");
        message.setSubject("Account activation.");
        message.setText(body);
        System.out.println(tokenID);
        javaMailSender.send(message);
    }

}
