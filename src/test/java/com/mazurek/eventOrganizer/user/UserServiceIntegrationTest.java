package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.AuthenticationResponse;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.city.CityNotFoundException;
import com.mazurek.eventOrganizer.exception.user.*;
import com.mazurek.eventOrganizer.jwt.*;
import com.mazurek.eventOrganizer.user.dto.ChangeUserDetailsDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserEmailDto;
import com.mazurek.eventOrganizer.user.dto.ChangeUserPasswordDto;
import com.mazurek.eventOrganizer.user.dto.UserProfileDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import javax.management.relation.RoleNotFoundException;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserServiceIntegrationTest {
    private final String USER_EMAIL = "example@dot.com";
    private final String USER_FIRST_NAME = "Andrew";
    private final String USER_LAST_NAME = "Golota";
    private final String USER_PASSWORD = "Password123!";
    private final String USER_TIME_ZONE = "Europe/Warsaw";
    private final String CITY_RZESZOW_NAME = "Rzeszow".toLowerCase(Locale.ROOT);

    private final String ROLE_USER_NAME = "ROLE_USER";
    private final String ROLE_ADMIN_NAME = "ROLE_ADMIN";


    @Autowired
    private UserService userService;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() throws RoleNotFoundException {
        if (roleRepository.findByName(ROLE_USER_NAME).isEmpty()){
            Role roleUser = new Role(ROLE_USER_NAME);
            roleRepository.save(roleUser);
        }
        if(roleRepository.findByName(ROLE_ADMIN_NAME).isEmpty()){
            Role roleAdmin = new Role(ROLE_ADMIN_NAME);
            roleRepository.save(roleAdmin);
        }

        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        cityRepository.deleteAll();

        City cityRzeszow = cityRepository.save(new City(CITY_RZESZOW_NAME));

        Role roleUser = roleRepository.findByName(ROLE_USER_NAME)
                .orElseThrow(RoleNotFoundException::new);

        Instant userCreateDateTime = Instant.now();

        User user = userRepository.save(User.builder()
                .firstName(USER_FIRST_NAME)
                .lastName(USER_LAST_NAME)
                .email(USER_EMAIL)
                .homeCity(cityRzeszow)
                .password(passwordEncoder.encode(USER_PASSWORD))
                .createdAt(userCreateDateTime)
                .timeZone(USER_TIME_ZONE)
                .roles(new HashSet<>(Set.of(roleUser)))
                .lastCredentialsChangeTime(userCreateDateTime)
                .activated(true)
                .banned(false)
                .build());

        cityRzeszow.addResident(user);
        cityRepository.save(cityRzeszow);

        authenticateUser();
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        cityRepository.deleteAll();

        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(){

        User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);

        JwtUserDetails userDetails = new JwtUserDetails(user.getId(),
                USER_EMAIL,
                user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).toList()
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails,
                null,
                userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Nested
    @DisplayName("Get user by id tests:")
    class GetUserByIdTests{

    }

    @Nested
    @DisplayName("Change user details tests tests:")
    class ChangeUserDetailsTests {

        private final String NEW_FIRST_NAME = "Nicolas";
        private final String NEW_LAST_NAME = "Klatka";
        private final String NEW_CITY_NAME = "Krakow";

        private ChangeUserDetailsDto changeUserDetailsDto;

        @BeforeEach
        void setUp() {

            changeUserDetailsDto = ChangeUserDetailsDto.builder()
                    .firstName(NEW_FIRST_NAME).lastName(NEW_LAST_NAME).homeCity(NEW_CITY_NAME).build();
        }

        @AfterEach
        void tearDown() {

        }

        @Test
        @DisplayName("When changing user details should throw exception if user not authenticated")
        public void whenChangingUserDetailsShouldThrowExceptionIfUserNotAuthenticated() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> userService.changeDetails(changeUserDetailsDto))
                    .isInstanceOf(UserNotAuthenticatedException.class);
        }

        @Test
        @DisplayName("When changing user details should not change city if name differs only in case")
        public void whenChangingUserDetailsShouldNotChangeCityIfNameDiffersOnlyInCase() {

            changeUserDetailsDto.setHomeCity(CITY_RZESZOW_NAME.toUpperCase());

            City originalCity = cityRepository.findByIgnoreCaseName(CITY_RZESZOW_NAME)
                    .orElseThrow();

            userService.changeDetails(changeUserDetailsDto);

            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);

            assertThat(user.getHomeCity().getId()).isEqualTo(originalCity.getId());

            assertThat(cityRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("When changing user details should update user first name and last name ")
        public void whenChangingUserDetailsShouldUpdateUserFirstNameAndLastName() {
            changeUserDetailsDto.setHomeCity(CITY_RZESZOW_NAME);

            userService.changeDetails(changeUserDetailsDto);

            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);

            assertThat(user)
                    .extracting(User::getFirstName, User::getLastName)
                    .containsExactly(NEW_FIRST_NAME, NEW_LAST_NAME);

        }

        @Test
        @DisplayName("When changing user details should update City")
        public void whenChangingUserDetailsShouldUpdateCity() {
            userService.changeDetails(changeUserDetailsDto);

            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow(UserNotFoundException::new);

            assertThat(cityRepository.findByIgnoreCaseName(NEW_CITY_NAME).isPresent())
                    .isTrue();

            assertThat(user.getHomeCity().getName())
                    .isEqualTo(NEW_CITY_NAME.toLowerCase(Locale.ROOT));

        }

        @Test
        @DisplayName("When changing user details should create new city if it doesn't exist")
        public void whenChangingUserDetailsShouldCreateNewCityIfItDoesntExist() {
            assertThat(cityRepository.findByIgnoreCaseName(NEW_CITY_NAME)).isEmpty();

            long cityCountBefore = cityRepository.count();

            userService.changeDetails(changeUserDetailsDto);


            assertThat(cityRepository.count()).isEqualTo(cityCountBefore + 1);
            assertThat(cityRepository.findByIgnoreCaseName(NEW_CITY_NAME)).isPresent();
        }

        @Test
        @DisplayName("When changing user details should reuse existing city instead of creating duplicate")
        public void whenChangingUserDetailsShouldReuseExistingCityInsteadOfCreatingDuplicate() {

            City existingCity = cityRepository.save(new City(NEW_CITY_NAME.toLowerCase()));
            long cityCountBefore = cityRepository.count();

            userService.changeDetails(changeUserDetailsDto);

            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);


            assertThat(cityRepository.count()).isEqualTo(cityCountBefore);
            assertThat(user.getHomeCity().getId()).isEqualTo(existingCity.getId());
        }

        @Test
        @DisplayName("When changing user details should persist changes in database")
        public void whenChangingUserDetailsShouldPersistChangesInDatabase() {
            userService.changeDetails(changeUserDetailsDto);

            userRepository.flush();

            User reloadedUser = userRepository.findByIgnoreCaseEmail(USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new);

            assertThat(reloadedUser)
                    .extracting(User::getFirstName, User::getLastName)
                    .containsExactly(NEW_FIRST_NAME, NEW_LAST_NAME);

            assertThat(reloadedUser.getHomeCity().getName())
                    .isEqualToIgnoringCase(NEW_CITY_NAME);
        }

        @Test
        @DisplayName("When changing user details should returned updated user profile dto")
        public void whenChangingUserDetailsShouldReturnUpdatedUserProfileDto() {
            UserProfileDto result = userService.changeDetails(changeUserDetailsDto);

            assertThat(result).isNotNull();
            assertThat(result.getFirstName())
                    .isEqualTo(NEW_FIRST_NAME);
            assertThat(result.getLastName())
                    .isEqualTo(NEW_LAST_NAME);
            assertThat(result.getHomeCity())
                    .isEqualTo(NEW_CITY_NAME.toLowerCase(Locale.ROOT));

        }

    }

    @Nested
    @DisplayName("Change password integration tests:")
    class ChangePasswordTests {
        private final String USER_PASSWORD_WRONG = "wrongPassword!123";
        private final String NEW_PASSWORD = "NewPassword!123";
        private final DeviceType deviceType = DeviceType.WEB;
        private final String deviceInfo = "RandomUserAgent";

        private ChangeUserPasswordDto changeUserPasswordDto;

        @BeforeEach
        void setUp() {
            changeUserPasswordDto = new ChangeUserPasswordDto(
                    NEW_PASSWORD, NEW_PASSWORD, USER_PASSWORD
            );
        }

        @Test
        @DisplayName("When changing password should update password hash in database")
        public void whenChangingPasswordShouldUpdatePasswordHashInDatabase(){
            User userBefore = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow();
            String oldPasswordHash = userBefore.getPassword();

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            User userAfter = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow();

            assertThat(userAfter.getPassword()).isNotEqualTo(oldPasswordHash);
        }

        @Test
        @DisplayName("When changing password should persist password hash that works with new password")
        public void whenChangingPasswordShouldPersistPasswordHashThatWorksWithNewPassword(){
            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            User userAfter = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow();

            assertThat(passwordEncoder.matches(NEW_PASSWORD, userAfter.getPassword())).isTrue();
        }

        @Test
        @DisplayName("When changing password should make old password invalid")
        public void whenChangingPasswordShouldMakeOldPasswordInvalid(){
            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            User userAfter = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow();

            assertThat(passwordEncoder.matches(USER_PASSWORD, userAfter.getPassword())).isFalse();
        }

        @Test
        @DisplayName("When changing password should update last credentials change time in database")
        public void whenChangingPasswordShouldUpdateLastCredentialsChangeTimeInDatabase(){
            User userBefore = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow();
            Instant lastCredentialsChangeBefore = userBefore.getLastCredentialsChangeTime();

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            User userAfter = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow();

            assertThat(userAfter.getLastCredentialsChangeTime()).isAfter(lastCredentialsChangeBefore);
        }

        @Test
        @DisplayName("When changing password should revoke all existing refresh tokens in database")
        public void whenChangingPasswordShouldRevokeAllExistingRefreshTokensInDatabase(){
            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow();

            RefreshToken oldToken1 = refreshTokenService.createRefreshToken(user, DeviceType.WEB);
            RefreshToken oldToken2 = refreshTokenService.createRefreshToken(user, DeviceType.MOBILE_ANDROID);

            userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            RefreshToken token1After = refreshTokenRepository.findByToken(oldToken1.getToken()).orElseThrow();
            RefreshToken token2After = refreshTokenRepository.findByToken(oldToken2.getToken()).orElseThrow();

            assertThat(token1After.isRevoked()).isTrue();
            assertThat(token2After.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("When changing password should create new refresh token with correct properties")
        public void whenChangingPasswordShouldCreateNewRefreshTokenWithCorrectProperties(){
            AuthenticationResponse response = userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            RefreshToken newToken = refreshTokenRepository.findByToken(response.getRefreshToken())
                    .orElseThrow();

            assertThat(newToken.getDeviceType()).isEqualTo(deviceType);
            assertThat(newToken.isRevoked()).isFalse();
            assertThat(newToken.isExpired()).isFalse();
        }

        @Test
        @DisplayName("When changing password should return valid access token")
        public void whenChangingPasswordShouldReturnValidAccessToken(){
            AuthenticationResponse response = userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            assertThat(jwtUtils.isTokenValid(response.getAccessToken())).isTrue();
            assertThat(jwtUtils.extractUsername(response.getAccessToken())).isEqualTo(USER_EMAIL);
        }

        @Test
        @DisplayName("When changing password should return complete authentication response")
        public void whenChangingPasswordShouldReturnCompleteAuthenticationResponse(){
            AuthenticationResponse response = userService.changePassword(changeUserPasswordDto, deviceType, deviceInfo);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isNotBlank();
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getAccessTokenExpiration()).isPositive();
        }
    }

    @Nested
    @DisplayName("Change email tests:")
    class ChangeEmailTests {
        private final String NEW_EMAIL = "new@mail.com";
        private final DeviceType deviceType = DeviceType.WEB;
        private final String deviceInfo = "RandomUserAgent";
        private ChangeUserEmailDto changeUserEmailDto;

        @BeforeEach
        void setUp() {
            changeUserEmailDto = new ChangeUserEmailDto(NEW_EMAIL, NEW_EMAIL, USER_PASSWORD);
        }
        private void createAnotherUserWithEmail(String email) {
            Instant userCreateDateTime = Instant.now();
            User user = userRepository.save(User.builder()
                    .firstName(USER_FIRST_NAME)
                    .lastName(USER_LAST_NAME)
                    .email(email)
                    .homeCity(cityRepository.findByIgnoreCaseName(CITY_RZESZOW_NAME).orElseThrow(CityNotFoundException::new))
                    .password(passwordEncoder.encode(USER_PASSWORD))
                    .createdAt(userCreateDateTime)
                    .timeZone(USER_TIME_ZONE)
                    .roles(new HashSet<>(Set.of(roleRepository.findByName(ROLE_USER_NAME).orElseThrow(UserRoleNotFoundException::new))))
                    .lastCredentialsChangeTime(userCreateDateTime)
                    .activated(true)
                    .banned(false)
                    .build());
        }

        @Test
        @DisplayName("When changing email to existing one should fail transactionally")
        void whenChangingEmailShouldFailWhenEmailAlreadyExistsInDatabase() {
            String existingEmail = "existing@mail.com";
            createAnotherUserWithEmail(existingEmail);

            changeUserEmailDto.setNewEmail("existing@mail.com");
            changeUserEmailDto.setNewEmailConfirmation("existing@mail.com");

            assertThatThrownBy(() ->
                    userService.changeEmail(changeUserEmailDto, deviceType, deviceInfo)
            ).isInstanceOf(UserAlreadyExistException.class);
        }

        @Test
        @DisplayName("When changing email should persist updated user")
        public void whenChangingEmailShouldPersistUpdatedUser(){
            UUID userId = userRepository.findByIgnoreCaseEmail(USER_EMAIL)
                    .orElseThrow(UserNotFoundException::new)
                    .getId();

            userService.changeEmail(changeUserEmailDto, deviceType, deviceInfo);

            assertThat(userRepository.findByEmail(NEW_EMAIL).isPresent())
                    .isTrue();
            assertThat(userRepository.findByEmail(NEW_EMAIL).orElseThrow(UserNotFoundException::new).getId())
                    .isEqualTo(userId);

        }
        @Test
        @DisplayName("When changing email should not find user by old email")
        public void whenChangingEmailShouldNotFindUserByOldEmail(){
            userService.changeEmail(changeUserEmailDto, deviceType, deviceInfo);

            assertThat(userRepository.findByEmail(USER_EMAIL).isEmpty())
                    .isTrue();

        }


        @Test
        @DisplayName("When changing email should update last credentials change time in database")
        public void whenChangingEmailShouldUpdateLastCredentialsChangeTimeInDatabase(){
            User userBefore = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow();
            Instant lastCredentialsChangeBefore = userBefore.getLastCredentialsChangeTime();

            userService.changeEmail(changeUserEmailDto, deviceType, deviceInfo);

            User userAfter = userRepository.findByIgnoreCaseEmail(NEW_EMAIL).orElseThrow();

            assertThat(userAfter.getLastCredentialsChangeTime()).isAfter(lastCredentialsChangeBefore);
        }

        @Test
        @DisplayName("When changing email should revoke all existing refresh tokens in database")
        public void whenChangingEmailShouldRevokeAllExistingRefreshTokensInDatabase(){
            User user = userRepository.findByIgnoreCaseEmail(USER_EMAIL).orElseThrow();

            RefreshToken oldToken1 = refreshTokenService.createRefreshToken(user, DeviceType.WEB);
            RefreshToken oldToken2 = refreshTokenService.createRefreshToken(user, DeviceType.MOBILE_ANDROID);

            userService.changeEmail(changeUserEmailDto, deviceType, deviceInfo);

            RefreshToken token1After = refreshTokenRepository.findByToken(oldToken1.getToken()).orElseThrow();
            RefreshToken token2After = refreshTokenRepository.findByToken(oldToken2.getToken()).orElseThrow();

            assertThat(token1After.isRevoked()).isTrue();
            assertThat(token2After.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("When changing email should create new refresh token with correct properties")
        public void whenChangingEmailShouldCreateNewRefreshTokenWithCorrectProperties(){
            AuthenticationResponse response = userService.changeEmail(changeUserEmailDto, deviceType, deviceInfo);

            RefreshToken newToken = refreshTokenRepository.findByToken(response.getRefreshToken())
                    .orElseThrow();

            assertThat(newToken.getDeviceType()).isEqualTo(deviceType);
            assertThat(newToken.isRevoked()).isFalse();
            assertThat(newToken.isExpired()).isFalse();
        }

        @Test
        @DisplayName("When changing email should return valid access token")
        public void whenChangingEmailShouldReturnValidAccessToken(){
            AuthenticationResponse response = userService.changeEmail(changeUserEmailDto, deviceType, deviceInfo);

            assertThat(jwtUtils.isTokenValid(response.getAccessToken())).isTrue();
            assertThat(jwtUtils.extractUsername(response.getAccessToken())).isEqualTo(NEW_EMAIL);
        }

        @Test
        @DisplayName("When changing email should return complete authentication response")
        public void whenChangingEmailShouldReturnCompleteAuthenticationResponse(){
            AuthenticationResponse response = userService.changeEmail(changeUserEmailDto, deviceType, deviceInfo);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isNotBlank();
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getAccessTokenExpiration()).isPositive();
        }
    }


    @Nested
    @DisplayName("Register user fcm token tests:")
    class RegisterUserFcmTokenTests {
        //TODO
        // FCM TOKEN REGISTER TESTS
    }

    @Nested
    @DisplayName("Ban user tests:")
    class BanUserTests{
        //TODO
        // BAN USER TESTS
    }

}
