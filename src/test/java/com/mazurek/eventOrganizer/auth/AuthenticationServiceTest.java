package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    public static final String CORRECT_EMAIL = "example@dot.com";
    @Mock private  UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder = Mockito.spy(new BCryptPasswordEncoder());
    @Mock private  JwtUtil jwtUtil;
    @Mock private  CityUtils cityUtils;
    @Mock private  AuthenticationManager authenticationManager;

    private AuthenticationService authenticationService;
    private RegisterRequest registerRequest;
    private AuthenticationRequest authenticationRequest;
    private Optional<User> userOptional;

    @Mock VerificationTokenRepository verificationTokenRepository;
    @Mock JavaMailSender javaMailSender;

    private UUID userId = UUID.randomUUID();
    private UUID cityId = UUID.randomUUID();

    @BeforeEach
    void setUp() {


        authenticationService = Mockito.spy(new AuthenticationServiceImpl(userRepository, passwordEncoder, jwtUtil, cityUtils, authenticationManager, verificationTokenRepository, javaMailSender));

        registerRequest = RegisterRequest.builder()
                .email(CORRECT_EMAIL)
                .emailConfirmation(CORRECT_EMAIL)
                .firstName("Andrew")
                .lastName("Golota")
                .homeCity("Rzeszow")
                .password("password")
                .passwordConfirmation("password")
                .build();

        authenticationRequest = AuthenticationRequest.builder()
                .email(CORRECT_EMAIL)
                .password("password")
                .build();

        userOptional = Optional.of(
                User.builder()
                        .id(userId)
                        .email(CORRECT_EMAIL)
                        .role(Role.USER)
                        .firstName("Andrew")
                        .lastName("Golota")
                        .homeCity(new City(cityId,"Rzeszow",new ArrayList<>(), new HashSet<>()))
                        .attendingEvents(new ArrayList<>())
                        .userEvents(new ArrayList<>())
                        .password(passwordEncoder.encode("password"))
                        .lastCredentialsChangeTime(Calendar.getInstance().getTimeInMillis())
                        .build()
        );

    }

    /*
     ********************************************************************************************************************
     *                                       REGISTER TESTS
     ********************************************************************************************************************
     */

    @Test
    public void whenRegisteringShouldThrowUserAlreadyExistExceptionIfEmailIsInDatabase(){
        userOptional.get().setEmail(CORRECT_EMAIL);
        when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(userOptional);

        UserAlreadyExistException userAlreadyExistException = assertThrows(UserAlreadyExistException.class
                , () -> authenticationService.register(registerRequest));
    }

    @Test
    public void whenRegisteringShouldThrowNotMatchingPasswordExceptionIfPasswordAndConfirmationAreDifferent(){
        when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(Optional.empty());

        registerRequest.setPasswordConfirmation("incorrectPassword");

        NotMatchingPasswordsException notMatchingPasswordsException = assertThrows(NotMatchingPasswordsException.class
                , () -> authenticationService.register(registerRequest));
    }
    @Test
    public void whenRegisteringShouldThrowInvalidEmailExceptionIfEmailAndConfirmationAreDifferent(){
        when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(Optional.empty());

        registerRequest.setEmailConfirmation("wrongEmail@example.com");

        InvalidEmailException invalidEmailException = assertThrows(InvalidEmailException.class
                , () -> authenticationService.register(registerRequest));
    }

    @Test
    public void whenRegisteringShouldCreateUserObjectWithDataFromRegisterRequest(){
        when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(Optional.empty());
        when(cityUtils.resolveCity(anyString())).thenReturn(new City(registerRequest.getHomeCity()));


        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

        authenticationService.register(registerRequest);

        verify(userRepository).save(userArgumentCaptor.capture());
        User capturedUser = userArgumentCaptor.getValue();

        assertEquals(registerRequest.getEmail(), capturedUser.getEmail());
        assertEquals(registerRequest.getFirstName(), capturedUser.getFirstName());
        assertEquals(registerRequest.getLastName(), capturedUser.getLastName());
        assertEquals(registerRequest.getHomeCity(), capturedUser.getHomeCity().getName());
        assertTrue(passwordEncoder.matches(registerRequest.getPassword(), capturedUser.getPassword()));
        assertNotNull(capturedUser.getLastCredentialsChangeTime());
    }

    @Test
    public void whenRegisteringShould(){
        when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(Optional.empty());
        when(cityUtils.resolveCity(anyString())).thenReturn(new City(registerRequest.getHomeCity()));


        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);

        authenticationService.register(registerRequest);

        verify(userRepository).save(userArgumentCaptor.capture());
        User capturedUser = userArgumentCaptor.getValue();

        assertEquals(registerRequest.getEmail(), capturedUser.getEmail());
        assertEquals(registerRequest.getFirstName(), capturedUser.getFirstName());
        assertEquals(registerRequest.getLastName(), capturedUser.getLastName());
        assertEquals(registerRequest.getHomeCity(), capturedUser.getHomeCity().getName());
        assertTrue(passwordEncoder.matches(registerRequest.getPassword(), capturedUser.getPassword()));
        assertNotNull(capturedUser.getLastCredentialsChangeTime());
    }
    @Test
    public void whenRegisteringShouldSaveUserInDatabase(){
        when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(Optional.empty());

        authenticationService.register(registerRequest);

        verify(userRepository,times(1)).save(any(User.class));
    }

    @Test
    public void whenRegisteringShouldGenerateJwtTokenOnce(){
        when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(userOptional.get());

        authenticationService.register(registerRequest);

        verify(jwtUtil,times(1)).generateToken(any(User.class));
    }

    @Test
    public void whenRegisteringShouldReturnAuthenticationResponseContainingJwtToken(){
        when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(userOptional.get());

        var output = authenticationService.register(registerRequest);


    }

    /*
     ********************************************************************************************************************
     *                                       AUTHENTICATE TESTS
     ********************************************************************************************************************
     */

    @Test
    public void whenAuthenticatingShouldThrowUserNotFoundExceptionIfThereIsNoUserWithThatEmail(){
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        UserNotFoundException userNotFoundException = assertThrows(UserNotFoundException.class,
                () -> authenticationService.authenticate(authenticationRequest));
    }
    @Test
    public void whenAuthenticatingShouldThrowInvalidPasswordExceptionIfUserPassedWrongWrongPassword() {
        when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(userOptional);

        authenticationRequest.setPassword("wrongPassword");

        InvalidPasswordException authenticationException = assertThrows(InvalidPasswordException.class,
                () -> authenticationService.authenticate(authenticationRequest));
    }
    @Test
    public void whenAuthenticatingShouldCreateNewUsernamePasswordAuthenticationToken(){
        when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(userOptional);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenArgumentCaptor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);

        authenticationService.authenticate(authenticationRequest);

        verify(authenticationManager).authenticate(tokenArgumentCaptor.capture());

        UsernamePasswordAuthenticationToken authenticationToken = tokenArgumentCaptor.getValue();

        assertEquals(authenticationRequest.getEmail(), authenticationToken.getPrincipal());
        assertEquals(authenticationRequest.getPassword(), authenticationToken.getCredentials());

    }

    @Test
    public void whenAuthenticatingShouldAuthenticateUserViaAuthenticationManager(){
        when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(userOptional);

        authenticationService.authenticate(authenticationRequest);

        verify(authenticationManager,times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
    @Test
    public void whenAuthenticatingShouldGenerateJwtToken(){
        when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(userOptional);

        authenticationService.authenticate(authenticationRequest);

        verify((jwtUtil),times(1)).generateToken(any(User.class));
    }

    @Test
    public void whenAuthenticatingShouldReturnAuthenticationResponse(){
        when(userRepository.findByEmail(CORRECT_EMAIL)).thenReturn(userOptional);

        var output = authenticationService.authenticate(authenticationRequest);

       assertEquals(AuthenticationResponse.class, output.getClass());
    }
}