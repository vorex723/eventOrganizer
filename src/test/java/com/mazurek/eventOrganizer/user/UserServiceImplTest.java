package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.AuthenticationResponse;
import com.mazurek.eventOrganizer.auth.AuthenticationServiceImpl;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityUtils;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.user.dto.ChangeUserDetailsDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserEmailDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserPasswordDto;
import com.mazurek.eventOrganizer.user.dto.UserWithEventsDto;
import com.mazurek.eventOrganizer.user.mapper.UserMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    public static final String EMAIL = "example@dot.com";
    public static final String NEW_EMAIL = "witam@witam.pl";
    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;

    private BCryptPasswordEncoder passwordEncoder = Mockito.spy(new BCryptPasswordEncoder());

    //@Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private AuthenticationServiceImpl authenticationService;
    @Mock private UserMapper userMapper;
    @Mock private CityUtils cityUtils;
    private UserServiceImpl userService;
    private Optional<User> userOptional;
    private ChangeUserPasswordDto changeUserPasswordDto;
    private ChangeUserEmailDto changeUserEmailDto;
    private ChangeUserDetailsDto changeUserDetailsDto;


    @BeforeEach
    void setUp() {
        userOptional = Optional.of(
                User.builder()
                        .id(1L)
                        .email("example@dot.com")
                        .role(Role.USER)
                        .firstName("Andrew")
                        .lastName("Golota")
                        .homeCity(new City(1L,"Rzeszow",new ArrayList<>(), new HashSet<>()))
                        .attendingEvents(new ArrayList<>())
                        .userEvents(new ArrayList<>())
                        .password(passwordEncoder.encode("password"))
                        .lastCredentialsChangeTime(Calendar.getInstance().getTimeInMillis())
                        .build()
        );

        userService = new UserServiceImpl(userRepository, jwtUtil, passwordEncoder,authenticationService, userMapper, cityUtils);

        changeUserPasswordDto = ChangeUserPasswordDto.builder()
                .newPassword("newPassword")
                .newPasswordConfirmation("newPassword")
                .password("password")
                .build();

        changeUserEmailDto = ChangeUserEmailDto.builder()
                .password("password")
                .newEmail("witam@witam.pl")
                .newEmailConfirmation("witam@witam.pl")
                .build();

        changeUserDetailsDto = ChangeUserDetailsDto.builder()
                .firstName("andrzej")
                .lastName("Konieczny")
                .homeCity("Krakow")
                .build();
    }

    @AfterEach
    void tearDown(){
    }

    @Test
    public void whenGettingUserByIdShouldRunQueryOnce(){

        when(userRepository.findById(anyLong())).thenReturn(userOptional);
        userService.getUserById(anyLong());
        verify(userRepository,times(1)).findById(anyLong());
    }

    @Test
    void whenGettingUserByIdShouldThrowUserNotFoundExceptionIfNoUser(){
       UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> userService.getUserById(2L));
    }

    /*
    ********************************************************************************************************************
    *                                       CHANGE USER PASSWORD TESTS
    ********************************************************************************************************************
    */

    @Nested
    @DisplayName("Change user password test")
    class ChangeUserPasswordTests{
        @Test
        void whenChangingPasswordShouldExtractUsernameFromJwtToken(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);

            userService.changeUserPassword(changeUserPasswordDto, anyString());

            verify(jwtUtil,times(1)).extractUsername(anyString());
        }

        @Test
        void whenChangingPasswordShouldRunQueryOnce(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);

            userService.changeUserPassword(changeUserPasswordDto, anyString());

            verify(userRepository, times(1)).findByEmail(anyString());
        }

        @Test
        void whenChangingPasswordShouldThrowInvalidPasswordExceptionIfOldPasswordIsWrong(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);

            changeUserPasswordDto.setPassword("wrongPassword");

            InvalidPasswordException exception = assertThrows(InvalidPasswordException.class,
                    () -> userService.changeUserPassword(changeUserPasswordDto, anyString()));
        }

        @Test
        void whenChangingPasswordShouldThrowNotMatchingPasswordsExceptionIfNewPasswordIsDifferentFromConfirmation(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);

            changeUserPasswordDto.setNewPassword("wrongPassword");

            NotMatchingPasswordsException exception = assertThrows(NotMatchingPasswordsException.class,
                    () -> userService.changeUserPassword(changeUserPasswordDto,anyString()));
        }

        @Test
        void whenChangingPasswordShouldEncodePasswordWhileSetting(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);

            userService.changeUserPassword(changeUserPasswordDto,anyString());

            verify(passwordEncoder, times(2)).encode(anyString());
        }
        @Test

        void whenChangingPasswordShouldSetPassword() {
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);

            userService.changeUserPassword(changeUserPasswordDto, anyString());

            assertTrue(passwordEncoder.matches("newPassword", userOptional.get().getPassword()));

        }

        @Test
        void whenChangingPasswordShouldUpdateLastCredentialChangeTimeField(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);


            Long lastCredentialChangeTime = userOptional.get().getLastCredentialsChangeTime();
            userService.changeUserPassword(changeUserPasswordDto, anyString());

            assertTrue(lastCredentialChangeTime<userOptional.get().getLastCredentialsChangeTime());
        }

        @Test
        void whenChangingPasswordShouldSaveUpdatedUser()
        {
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);

            userService.changeUserPassword(changeUserPasswordDto,anyString());

            verify(userRepository,times(1)).save(any(User.class));
        }

        @Test
        void whenChangingPasswordShouldGenerateNewTokenForUser(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);
            when(userRepository.save(any(User.class))).thenReturn(userOptional.get());

            userService.changeUserPassword(changeUserPasswordDto,anyString());

            verify(jwtUtil, times(1)).generateToken(any(User.class));
        }
        @Test
        void whenChangingPasswordShouldReturnAuthenticationResponseObject(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(anyString())).thenReturn(userOptional);

            var output = userService.changeUserPassword(changeUserPasswordDto,anyString());

            assertNotNull(output);
            assertEquals(output.getClass(), AuthenticationResponse.class);
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
        @Test
        void whenChangingUserEmailShouldThrowInvalidEmailExceptionIfNewEmailAndConfirmationsAreDifferent(){
            changeUserEmailDto.setNewEmailConfirmation("wrongEmail@example.com");
            InvalidEmailException exception = assertThrows(InvalidEmailException.class
                    ,() -> userService.changeUserEmail(changeUserEmailDto,"exampleJwtToken"));
        }

        @Test
        void whenChangingUserEmailShouldThrowUserAlreadyExistExceptionIfEmailIsAlreadyInDatabase(){
            userOptional.get().setEmail(NEW_EMAIL);
            when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(userOptional);

            changeUserEmailDto.setPassword("wrongPassword");

            UserAlreadyExistException exception = assertThrows(UserAlreadyExistException.class
                    , () -> userService.changeUserEmail(changeUserEmailDto, anyString()));
        }
        @Test
        void whenChangingUserEmailShouldExtractUserEmailFromJwt(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.empty());

            userService.changeUserEmail(changeUserEmailDto, anyString());

            verify(jwtUtil,times(1)).extractUsername(anyString());

        }
        @Test
        void whenChangingUserEmailShouldRunQueryOnce(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.empty());

            userService.changeUserEmail(changeUserEmailDto, anyString());

            verify(userRepository,times(2)).findByEmail(anyString());
        }

        @Test
        void whenChangingUserEmailShouldCheckIfUserPutCorrectPassword(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.empty());

            userService.changeUserEmail(changeUserEmailDto, anyString());

            verify(passwordEncoder,times(1)).matches(anyString(),anyString());
        }

        @Test
        void whenChangingUserEmailShouldThrowInvalidPasswordExceptionIfUserPutWrongPassword(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.empty());

            changeUserEmailDto.setPassword("wrongPassword");

            InvalidPasswordException exception = assertThrows(InvalidPasswordException.class,
                    () -> userService.changeUserEmail(changeUserEmailDto, anyString()));
        }

        @Test
        void whenChangingUserEmailShouldUpdateUserEmail(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.empty());

            assertNotEquals(userOptional.get().getEmail(),changeUserEmailDto.getNewEmail());
            userService.changeUserEmail(changeUserEmailDto, anyString());

            assertEquals(userOptional.get().getEmail(),changeUserEmailDto.getNewEmail());
        }

        @Test
        void whenChangingUserEmailShouldUpdateLastCredentialChangeTime(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.empty());

            Long lastCredentialsUpdate = userOptional.get().getLastCredentialsChangeTime();
            userService.changeUserEmail(changeUserEmailDto, anyString());

            assertTrue(lastCredentialsUpdate < userOptional.get().getLastCredentialsChangeTime());
        }

        @Test
        void whenChangingUserEmailShouldUpdateUserInDatabase(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.empty());

            userService.changeUserEmail(changeUserEmailDto, anyString());

            verify(userRepository,times(1)).save(any(User.class));
        }

        @Test
        void whenChangingUserEmailShouldGenerateNewJwtToken(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.save(any(User.class))).thenReturn(userOptional.get());
            when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.empty());

            userService.changeUserEmail(changeUserEmailDto, anyString());

            verify(jwtUtil,times(1)).generateToken(any(User.class));
        }

        @Test
        void whenChangingUserEmailShouldReturnAuthenticationResponseObject(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.findByEmail(NEW_EMAIL)).thenReturn(Optional.empty());

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
        @Test
        void whenChangingUserDetailsShouldExtractUsernameFromJwtToken(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);

            userService.changeUserDetails(changeUserDetailsDto,anyString());

            verify(jwtUtil,times(1)).extractUsername(anyString());
        }

        @Test
        void whenChangingUserDetailsShouldSetFirstName(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);

            userService.changeUserDetails(changeUserDetailsDto,anyString());

            assertEquals(changeUserDetailsDto.getFirstName(),userOptional.get().getFirstName());
        }

        @Test
        void whenChangingUserDetailsShouldSetLastName(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);

            userService.changeUserDetails(changeUserDetailsDto,anyString());

            assertEquals(changeUserDetailsDto.getLastName(),userOptional.get().getLastName());
        }

        @Test
        void whenChangingUserDetailsShouldResolveCity(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);

            userService.changeUserDetails(changeUserDetailsDto,anyString());

            verify(cityUtils, times(1)).resolveCity(anyString());
        }

        @Test
        void whenChangingUserDetailsShouldSetCity(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(cityUtils.resolveCity(anyString())).thenReturn(new City(changeUserDetailsDto.getHomeCity()));

            userService.changeUserDetails(changeUserDetailsDto,anyString());

            assertEquals(changeUserDetailsDto.getHomeCity(),userOptional.get().getHomeCity().getName());
        }

        @Test
        void whenChangingUserDetailsShouldSaveUpdatedUser(){

            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);

            userService.changeUserDetails(changeUserDetailsDto,anyString());

            verify(userRepository,times(1)).save(any(User.class));
        }
        @Test
        void whenChangingUserDetailsShouldMapSavedUserToDtoWithUserEvents(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.save(any(User.class))).thenReturn(userOptional.get());

            userService.changeUserDetails(changeUserDetailsDto,anyString());

            verify(userMapper,times(1)).mapUserToUserWithEventsDto(any(User.class));
        }

        @Test
        void whenChangingUserDetailsShouldReturnUserWithEventDtoObject(){
            when(jwtUtil.extractUsername(anyString())).thenReturn(EMAIL);
            when(userRepository.findByEmail(EMAIL)).thenReturn(userOptional);
            when(userRepository.save(any(User.class))).thenReturn(userOptional.get());
            when(userMapper.mapUserToUserWithEventsDto(any(User.class))).thenReturn(new UserWithEventsDto());

            var output = userService.changeUserDetails(changeUserDetailsDto,anyString());

            assertEquals(UserWithEventsDto.class, output.getClass());
        }

    }


}