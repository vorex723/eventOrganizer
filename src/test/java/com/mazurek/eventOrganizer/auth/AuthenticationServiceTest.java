package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    private final UUID USER_ID = UUID.randomUUID();
    private final UUID CITY_ID = UUID.randomUUID();
    private final UUID VERIFICATION_TOKEN_ID = UUID.randomUUID();

    private final String USER_EMAIL = "example@dot.com";
    private final String USER_FIRST_NAME = "Andrew";
    private final String USER_LAST_NAME = "Golota";
    private final String PASSWORD = "password";
    private final String CITY_NAME = "Rzeszow";
    private final String PLATFORM_EMAIL = "testowe.andrzej.testowe@gmail.com";

    private final long VERIFICATION_TOKEN_EXPIRATION_TIME_DAYS = 4;

    @Mock private  UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder = Mockito.spy(new BCryptPasswordEncoder());
    @Mock private  JwtUtil jwtUtil;
    @Mock private  CityUtils cityUtils;
    @Mock private  AuthenticationManager authenticationManager;
    private AuthenticationService authenticationService;
    private AuthenticationService authenticationServiceSpy;


    private RegisterRequest registerRequest;
    private AuthenticationRequest authenticationRequest;

    private User user;
    private Optional<User> userOptional;
    private VerificationToken verificationToken;

    private City cityRzeszow;

    @Mock VerificationTokenRepository verificationTokenRepository;
    @Mock JavaMailSender javaMailSender;


    @BeforeEach
    void setUp() {

        authenticationService = new AuthenticationServiceImpl(userRepository, passwordEncoder, jwtUtil, cityUtils, authenticationManager, verificationTokenRepository, javaMailSender);
        authenticationServiceSpy = Mockito.spy(authenticationService);

        registerRequest = RegisterRequest.builder()
                .email(USER_EMAIL)
                .emailConfirmation(USER_EMAIL)
                .firstName(USER_FIRST_NAME)
                .lastName(USER_LAST_NAME)
                .homeCity(CITY_NAME)
                .password(PASSWORD)
                .passwordConfirmation(PASSWORD)
                .build();

        authenticationRequest = AuthenticationRequest.builder()
                .email(USER_EMAIL)
                .password(PASSWORD)
                .build();

        user = User.builder()
                .id(USER_ID)
                .email(USER_EMAIL)
                .role(Role.USER)
                .firstName(USER_FIRST_NAME)
                .lastName(USER_LAST_NAME)
                .homeCity(new City(CITY_ID,CITY_NAME,new ArrayList<>(), new HashSet<>()))
                .attendingEvents(new ArrayList<>())
                .userEvents(new ArrayList<>())
                .password(passwordEncoder.encode(PASSWORD))
                .lastCredentialsChangeTime(LocalDateTime.now())
                .build();

        userOptional = Optional.of(user);

        verificationToken = new VerificationToken(VERIFICATION_TOKEN_ID, user, LocalDateTime.now().plusDays(VERIFICATION_TOKEN_EXPIRATION_TIME_DAYS));

        cityRzeszow = City.builder()
                .id(CITY_ID)
                .name(CITY_NAME)
                .build();

    }

    /*
     ********************************************************************************************************************
     *                                       REGISTER TESTS
     ********************************************************************************************************************
     */

    @Nested
    @DisplayName("Register new user tests:")
    class RegisterNewUserTests{
        @Test
        @DisplayName("When registering should throw UserAlreadyExistException if email is in database")
        public void whenRegisteringShouldThrowUserAlreadyExistExceptionIfEmailIsInDatabase(){
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(userOptional);

           assertThrows(UserAlreadyExistException.class, () -> authenticationService.register(registerRequest));
        }

        @Test
        @DisplayName("When registering should throw NotMatchingPasswordsException if password and confirmations are different")
        public void whenRegisteringShouldThrowNotMatchingPasswordsExceptionIfPasswordAndConfirmationAreDifferent(){
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

            registerRequest.setPasswordConfirmation("incorrectPassword");

           assertThrows(NotMatchingPasswordsException.class, () -> authenticationService.register(registerRequest));
        }

        @Test
        @DisplayName("When registering should throw NotMatchingEmailsException if email and confirmation are different")
        public void whenRegisteringShouldThrowNotMatchingEmailsExceptionIfEmailAndConfirmationAreDifferent(){
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());

            registerRequest.setEmailConfirmation("wrongEmail@example.com");

            assertThrows(NotMatchingEmailsException.class, () -> authenticationService.register(registerRequest));
        }

        @Test
        @DisplayName("When registering should save user object with data from register request")
        public void whenRegisteringShouldSaveUserObjectWithDataFromRegisterRequest(){
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());
            when(cityUtils.resolveCity(CITY_NAME)).thenReturn(cityRzeszow);
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(verificationToken);

            ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

            authenticationService.register(registerRequest);

            verify(userRepository, times(1)).save(userArgumentCaptor.capture());

            User capturedUser = userArgumentCaptor.getValue();

            assertEquals(registerRequest.getEmail(), capturedUser.getEmail());
            assertEquals(registerRequest.getFirstName(), capturedUser.getFirstName());
            assertEquals(registerRequest.getLastName(), capturedUser.getLastName());
            assertEquals(registerRequest.getHomeCity(), capturedUser.getHomeCity().getName());
            assertTrue(passwordEncoder.matches(registerRequest.getPassword(), capturedUser.getPassword()));
            assertNotNull(capturedUser.getLastCredentialsChangeTime());
        }

        @Test
        @DisplayName("When registering should generate verification token for user and save it")
        public void whenRegisteringShouldGenerateVerificationTokenAndSaveIt(){
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(verificationToken);

            ArgumentCaptor<VerificationToken> verificationTokenArgumentCaptor = ArgumentCaptor.forClass(VerificationToken.class);

            authenticationService.register(registerRequest);

            verify(verificationTokenRepository,times(1)).save(verificationTokenArgumentCaptor.capture());

            VerificationToken capturedToken = verificationTokenArgumentCaptor.getValue();

            assertEquals(user, capturedToken.getUser());
        }

        @Test
        @DisplayName("When registering should send email to user with account verification link")
        public void whenRegisteringShouldSendEmailToUserWithAccountVerificationLink() throws MessagingException, IOException {
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenReturn(userOptional.get());
            when(verificationTokenRepository.save(any(VerificationToken.class))).thenReturn(verificationToken);
            MimeMessage mimeMessage = mock(MimeMessage.class);
            when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

            ArgumentCaptor<MimeMessage> mimeMessageArgumentCaptor = ArgumentCaptor.forClass(MimeMessage.class);

            authenticationService.register(registerRequest);

            verify(javaMailSender, times(1)).send(mimeMessageArgumentCaptor.capture());

            MimeMessage capturedMessage = mimeMessageArgumentCaptor.getValue();

            //assertTrue(capturedMessage.getContent().toString().endsWith(VERIFICATION_TOKEN_ID.toString()));
            verify(mimeMessage,times(1)).setSubject(eq("Account activation."));
            verify(mimeMessage,times(1)).setContent(any(), eq("text/html; charset=utf-8"));
            verify(mimeMessage,times(1)).setRecipient(eq(MimeMessage.RecipientType.TO), any(InternetAddress.class));
            verify(javaMailSender,times(1)).createMimeMessage();
            verify(javaMailSender,times(1)).send(mimeMessage);
        }

    }

    /*
     ********************************************************************************************************************
     *                                       AUTHENTICATE TESTS
     ********************************************************************************************************************
     */
    @Nested
    @DisplayName("Authenticate user tests:")
    class AuthenticateUserTests{
        @Test
        @DisplayName("When authenticating should throw UserNotFoundException if there is no user with that email")
        public void whenAuthenticatingShouldThrowUserNotFoundExceptionIfThereIsNoUserWithThatEmail(){
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            UserNotFoundException userNotFoundException = assertThrows(UserNotFoundException.class,
                    () -> authenticationService.authenticate(authenticationRequest));
        }

        @Test
        @DisplayName("When authentication should throw InvalidPasswordException if user passed wrong password")
        public void whenAuthenticatingShouldThrowInvalidPasswordExceptionIfUserPassedWrongPassword() {
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(userOptional);

            authenticationRequest.setPassword("wrongPassword");

            InvalidPasswordException authenticationException = assertThrows(InvalidPasswordException.class,
                    () -> authenticationService.authenticate(authenticationRequest));
        }

        @Test
        @DisplayName("When authentication should create new UsernamePasswordAuthenticationToken")
        public void whenAuthenticatingShouldCreateNewUsernamePasswordAuthenticationToken(){
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(userOptional);

            ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenArgumentCaptor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);

            authenticationService.authenticate(authenticationRequest);

            verify(authenticationManager).authenticate(tokenArgumentCaptor.capture());

            UsernamePasswordAuthenticationToken authenticationToken = tokenArgumentCaptor.getValue();

            assertEquals(authenticationRequest.getEmail(), authenticationToken.getPrincipal());
            assertEquals(authenticationRequest.getPassword(), authenticationToken.getCredentials());

        }

        @Test
        @DisplayName("When authenticating should authenticate user via authentication manager")
        public void whenAuthenticatingShouldAuthenticateUserViaAuthenticationManager(){
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(userOptional);

            authenticationService.authenticate(authenticationRequest);

            verify(authenticationManager,times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("When authenticating should generate jwt token")
        public void whenAuthenticatingShouldGenerateJwtToken(){
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(userOptional);

            authenticationService.authenticate(authenticationRequest);

            verify((jwtUtil),times(1)).generateToken(any(User.class));
        }

        @Test
        @DisplayName("When authenticating should return AuthenticationResponse containing jwt token")
        public void whenAuthenticatingShouldReturnAuthenticationResponse(){
            when(userRepository.findByEmail(USER_EMAIL)).thenReturn(userOptional);

            var output = authenticationService.authenticate(authenticationRequest);

            assertEquals(AuthenticationResponse.class, output.getClass());
        }
    }

    @Nested
    @DisplayName("Activate account tests:")
    class ActivateAccountTests{

    }

    @Nested
    @DisplayName("Generate new account activation token tests:")
    class GenerateNewAccountActivationTokenTests{

    }

}