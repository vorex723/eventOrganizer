package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.AuthenticationResponse;
import com.mazurek.eventOrganizer.auth.AuthenticationServiceImpl;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.JwtUtils;
import com.mazurek.eventOrganizer.jwt.RefreshTokenService;
import com.mazurek.eventOrganizer.user.dto.ChangeUserDetailsDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserEmailDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserPasswordDto;
import com.mazurek.eventOrganizer.user.dto.UserWithEventsDto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private final String USER_NAME = "Andrew";
    private final String USER_LAST_NAME = "Golota";
    private final String CITY_RZESZOW_NAME = "Rzeszow";
    private final String USER_PASSWORD = "Password123!";
    private final UUID CITY_ID = UUID.randomUUID();

    @Mock private UserRepository userRepository;
    @Mock private AuthenticationServiceImpl authenticationService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private JwtUtils jwtUtils;
    @Mock private CityService cityService;
    private BCryptPasswordEncoder passwordEncoder = Mockito.spy(new BCryptPasswordEncoder());
    private UserService userService;

    private User user;
    private final UUID USER_ID = UUID.randomUUID();
    private final String USER_EMAIL = "example@dot.com";
    private final String USER_NEW_EMAIL = "witam@witam.pl";
    private final String USER_TIME_ZONE = "Europe/Warsaw";
    private City cityRzeszow;
    private Optional<User> userOptional;

    private Role ROLE_USER;
    private Long ROLE_USER_ID = 1L;
    private String ROLE_USER_NAME = "ROLE_USER";


    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, authenticationService, refreshTokenService, jwtUtils, cityService, passwordEncoder);

        ROLE_USER = new Role(ROLE_USER_ID, ROLE_USER_NAME);

        cityRzeszow = new City(CITY_ID, CITY_RZESZOW_NAME,new ArrayList<>(), new HashSet<>());
        Instant userCreateAccountTime = Instant.now();

        user = User.builder()
                .id(USER_ID)
                .email(USER_EMAIL)
                .roles(Set.of(ROLE_USER))
                .firstName(USER_NAME)
                .lastName(USER_LAST_NAME)
                .homeCity(cityRzeszow)
                .password(passwordEncoder.encode(USER_PASSWORD))
                .timeZone(USER_TIME_ZONE)
                .createdAt(userCreateAccountTime)
                .lastCredentialsChangeTime(userCreateAccountTime)
                .build();

        userOptional = Optional.of(user);

        cityRzeszow.addResident(user);
    }

    @AfterEach
    void tearDown(){
    }


    @Test
    public void whenGettingUserByIdShouldRunQueryOnce(){
        when(userRepository.findById(USER_ID)).thenReturn(userOptional);
        userService.getUserById(USER_ID);
        verify(userRepository,times(1)).findById(USER_ID);
    }

    @Test
    void whenGettingUserByIdShouldThrowUserNotFoundExceptionIfNoUser(){
       assertThrows(UserNotFoundException.class, () -> userService.getUserById(UUID.randomUUID()));
    }


/*
    ********************************************************************************************************************
    *                                       CHANGE USER PASSWORD TESTS
    ********************************************************************************************************************
    */


    @Nested
    @DisplayName("Change user password test")
    class ChangeUserPasswordTests{

        private ChangeUserPasswordDto changeUserPasswordDto;

        @BeforeEach
        void setUp() {

            changeUserPasswordDto = ChangeUserPasswordDto.builder()
                    .newPassword("newPassword")
                    .newPasswordConfirmation("newPassword")
                    .password("password")
                    .build();

        }

        @Test
        void whenChangingPasswordShouldExtractUsernameFromJwtToken(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);

            userService.changeUserPassword(changeUserPasswordDto, anyString());

            verify(jwtUtils,times(1)).extractUsername(anyString());
        }

        @Test
        void whenChangingPasswordShouldRunQueryOnce(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);

            userService.changeUserPassword(changeUserPasswordDto, anyString());

            verify(userRepository, times(1)).findByEmail(EMAIL);
        }

        @Test
        void whenChangingPasswordShouldThrowInvalidPasswordExceptionIfOldPasswordIsWrong(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);

            changeUserPasswordDto.setPassword("wrongPassword");

            assertThrows(InvalidPasswordException.class, () -> userService.changeUserPassword(changeUserPasswordDto, anyString()));
        }

        @Test
        void whenChangingPasswordShouldThrowNotMatchingPasswordsExceptionIfNewPasswordIsDifferentFromConfirmation(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);

            changeUserPasswordDto.setNewPassword("wrongPassword");

           assertThrows(NotMatchingPasswordsException.class, () -> userService.changeUserPassword(changeUserPasswordDto,anyString()));
        }

        @Test
        void whenChangingPasswordShouldEncodePasswordWhileSetting(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);

            userService.changeUserPassword(changeUserPasswordDto,anyString());

            verify(passwordEncoder, times(1)).encode(changeUserPasswordDto.getNewPassword());
        }
        @Test

        void whenChangingPasswordShouldSetPassword() {
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);

            userService.changeUserPassword(changeUserPasswordDto, anyString());

            assertTrue(passwordEncoder.matches("newPassword", userOptional.get().getPassword()));

        }

        @Test
        void whenChangingPasswordShouldUpdateLastCredentialChangeTimeField(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);


            LocalDateTime lastCredentialChangeTime = userOptional.get().getLastCredentialsChangeTime();
            userService.changeUserPassword(changeUserPasswordDto, anyString());

            assertTrue(lastCredentialChangeTime.isBefore(userOptional.get().getLastCredentialsChangeTime()));
        }

        @Test
        void whenChangingPasswordShouldSaveUpdatedUser()
        {
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);

            userService.changeUserPassword(changeUserPasswordDto,anyString());

            verify(userRepository,times(1)).save(any(User.class));
        }

        @Test
        void whenChangingPasswordShouldGenerateNewTokenForUser(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);
            when(userRepository.save(any(User.class))).thenReturn(userOptional.get());

            userService.changeUserPassword(changeUserPasswordDto,anyString());

            verify(jwtUtils, times(1)).generateToken(any(User.class));
        }
        @Test
        void whenChangingPasswordShouldReturnAuthenticationResponseObject(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);

            var output = userService.changeUserPassword(changeUserPasswordDto,anyString());

            assertNotNull(output);

        }


    }



/*
     ********************************************************************************************************************
     *                                      CHANGE USER EMAIL TESTS
     ********************************************************************************************************************
     */

    @Nested
    @DisplayName("Change user email tests")
    class ChangeUserEmailTest{
        private ChangeUserEmailDto changeUserEmailDto;

        @BeforeEach
        void setUp() {
            changeUserEmailDto = ChangeUserEmailDto.builder()
                    .password("password")
                    .newEmail("witam@witam.pl")
                    .newEmailConfirmation("witam@witam.pl")
                    .build();

        }

        @Test
        void whenChangingUserEmailShouldThrowInvalidEmailExceptionIfNewEmailAndConfirmationsAreDifferent(){
            changeUserEmailDto.setNewEmailConfirmation("wrongEmail@example.com");
            assertThrows(NotMatchingEmailsException.class, () -> userService.changeUserEmail(changeUserEmailDto,"exampleJwtToken"));
        }

        @Test
        void whenChangingUserEmailShouldThrowUserAlreadyExistExceptionIfEmailIsAlreadyInDatabase(){
            userOptional.get().setEmail(USER_NEW_EMAIL);
            when(userRepository.findByEmail(USER_NEW_EMAIL)).thenReturn(userOptional);

            changeUserEmailDto.setPassword("wrongPassword");
            assertThrows(UserAlreadyExistException.class, () -> userService.changeUserEmail(changeUserEmailDto, anyString()));
        }
        @Test
        void whenChangingUserEmailShouldExtractUserEmailFromJwt(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.findByEmail(USER_NEW_EMAIL)).thenReturn(Optional.empty());

            userService.changeUserEmail(changeUserEmailDto, anyString());

            verify(jwtUtils,times(1)).extractUsername(anyString());

        }
        @Test
        void whenChangingUserEmailShouldRunQueryOnce(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.findByEmail(USER_NEW_EMAIL)).thenReturn(Optional.empty());

            userService.changeUserEmail(changeUserEmailDto, anyString());

            verify(userRepository,times(2)).findByEmail(anyString());
        }

        @Test
        void whenChangingUserEmailShouldCheckIfUserPutCorrectPassword(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.findByEmail(USER_NEW_EMAIL)).thenReturn(Optional.empty());

            userService.changeUserEmail(changeUserEmailDto, anyString());

            verify(passwordEncoder,times(1)).matches(anyString(),anyString());
        }

        @Test
        void whenChangingUserEmailShouldThrowInvalidPasswordExceptionIfUserPutWrongPassword(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.findByEmail(USER_NEW_EMAIL)).thenReturn(Optional.empty());

            changeUserEmailDto.setPassword("wrongPassword");

            assertThrows(InvalidPasswordException.class, () -> userService.changeUserEmail(changeUserEmailDto, anyString()));
        }

        @Test
        void whenChangingUserEmailShouldUpdateUserEmail(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.findByEmail(USER_NEW_EMAIL)).thenReturn(Optional.empty());

            assertNotEquals(userOptional.get().getEmail(),changeUserEmailDto.getNewEmail());
            userService.changeUserEmail(changeUserEmailDto, anyString());

            assertEquals(userOptional.get().getEmail(),changeUserEmailDto.getNewEmail());
        }

        @Test
        void whenChangingUserEmailShouldUpdateLastCredentialChangeTime(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.findByEmail(USER_NEW_EMAIL)).thenReturn(Optional.empty());

            LocalDateTime lastCredentialsUpdate = userOptional.get().getLastCredentialsChangeTime();
            userService.changeUserEmail(changeUserEmailDto, anyString());

            assertTrue(lastCredentialsUpdate.isBefore(userOptional.get().getLastCredentialsChangeTime()));
        }

        @Test
        void whenChangingUserEmailShouldUpdateUserInDatabase(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.findByEmail(USER_NEW_EMAIL)).thenReturn(Optional.empty());

            userService.changeUserEmail(changeUserEmailDto, anyString());

            verify(userRepository,times(1)).save(any(User.class));
        }

        @Test
        void whenChangingUserEmailShouldGenerateNewJwtToken(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.save(any(User.class))).thenReturn(userOptional.get());
            when(userRepository.findByEmail(USER_NEW_EMAIL)).thenReturn(Optional.empty());

            userService.changeUserEmail(changeUserEmailDto, anyString());

            verify(jwtUtils,times(1)).generateToken(any(User.class));
        }

        @Test
        void whenChangingUserEmailShouldReturnAuthenticationResponseObject(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.findByEmail(USER_NEW_EMAIL)).thenReturn(Optional.empty());

            var output = userService.changeUserEmail(changeUserEmailDto, anyString());

            assertEquals(AuthenticationResponse.class, output.getClass());
        }

    }



/*
     ********************************************************************************************************************
     *                                       CHANGE USER DETAILS TESTS
     ********************************************************************************************************************
     */

    @Nested
    @DisplayName("Change user details tests")
    class ChangeUserDetails{

        private ChangeUserDetailsDto changeUserDetailsDto;

        @BeforeEach
        void setUp() {
            changeUserDetailsDto = ChangeUserDetailsDto.builder()
                    .firstName("andrzej")
                    .lastName("Konieczny")
                    .homeCity("Krakow")
                    .build();
        }

        @Test
        void whenChangingUserDetailsShouldExtractUsernameFromJwtToken(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.save(any(User.class))).thenReturn(user);

            userService.changeUserDetails(changeUserDetailsDto,anyString());

            verify(jwtUtils,times(1)).extractUsername(anyString());
        }

        @Test
        void whenChangingUserDetailsShouldSetFirstName(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.save(any(User.class))).thenReturn(user);

            userService.changeUserDetails(changeUserDetailsDto,anyString());

            assertEquals(changeUserDetailsDto.getFirstName(),userOptional.get().getFirstName());
        }

        @Test
        void whenChangingUserDetailsShouldSetLastName(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.save(any(User.class))).thenReturn(user);

            userService.changeDetails(changeUserDetailsDto);

            assertEquals(changeUserDetailsDto.getLastName(),userOptional.get().getLastName());
        }

        @Test
        void whenChangingUserDetailsShouldResolveCity(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.save(any(User.class))).thenReturn(user);

            userService.changeUserDetails(changeUserDetailsDto,anyString());

            verify(cityService, times(1)).getCityByNameOrCreate(anyString());
        }

        @Test
        void whenChangingUserDetailsShouldSetCity(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(cityService.getCityByNameOrCreate(anyString())).thenReturn(new City(changeUserDetailsDto.getHomeCity()));
            when(userRepository.save(any(User.class))).thenReturn(user);

            userService.changeUserDetails(changeUserDetailsDto,anyString());

            assertEquals(changeUserDetailsDto.getHomeCity().toLowerCase(),userOptional.get().getHomeCity().getName());
        }

        @Test
        void whenChangingUserDetailsShouldSaveUpdatedUser(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.save(any(User.class))).thenReturn(user);

            userService.changeUserDetails(changeUserDetailsDto,anyString());

            verify(userRepository,times(1)).save(any(User.class));
        }

        @Test
        void whenChangingUserDetailsShouldMapSavedUserToDtoWithUserEvents(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(cityService.getCityByNameOrCreate(anyString())).thenReturn(new City(changeUserDetailsDto.getHomeCity()));
            when(userRepository.save(any(User.class))).thenReturn(userOptional.get());

            userService.changeUserDetails(changeUserDetailsDto,anyString());

        }

        @Test
        void whenChangingUserDetailsShouldReturnUserWithEventDtoObject(){
            when(jwtUtils.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(cityService.getCityByNameOrCreate(anyString())).thenReturn(new City(changeUserDetailsDto.getHomeCity()));
            when(userRepository.save(any(User.class))).thenReturn(userOptional.get());


            var output = userService.changeUserDetails(changeUserDetailsDto,anyString());

            assertEquals(UserWithEventsDto.class, output.getClass());
        }

    }



}