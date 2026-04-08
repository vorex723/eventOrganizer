package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.testData.TestConstants;
import com.mazurek.eventOrganizer.testData.builders.RoleTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUserDetailsService unit tests:")
class JwtUserDetailsServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtUserDetailsService jwtUserDetailsService;

    @Nested
    @DisplayName("Load user by username tests:")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("When loading user by username should throw UsernameNotFoundException if user does not exist")
        void whenUserDoesNotExistShouldThrowUsernameNotFoundException() {
            when(userRepository.findByIgnoreCaseEmail(TestConstants.UserConstants.FIRST_USER_EMAIL))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> jwtUserDetailsService.loadUserByUsername(TestConstants.UserConstants.FIRST_USER_EMAIL))
                    .isInstanceOf(UsernameNotFoundException.class);
        }

        @Test
        @DisplayName("When loading user by username should map email password and roles to Spring Security user details")
        void whenUserExistsShouldMapEmailPasswordAndRolesToSpringSecurityUserDetails() {
            Role userRole = RoleTestBuilder.userRole().build();
            User user = UserTestBuilder.firstUser()
                    .password(TestConstants.UserConstants.USER_PASSWORD)
                    .build();
            user.setRoles(Set.of(userRole));
            user.setActivated(true);
            user.setBanned(false);

            when(userRepository.findByIgnoreCaseEmail(TestConstants.UserConstants.FIRST_USER_EMAIL))
                    .thenReturn(Optional.of(user));

            UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(TestConstants.UserConstants.FIRST_USER_EMAIL);

            assertThat(userDetails.getUsername()).isEqualTo(TestConstants.UserConstants.FIRST_USER_EMAIL);
            assertThat(userDetails.getPassword()).isEqualTo(TestConstants.UserConstants.USER_PASSWORD);
            assertThat(userDetails.getAuthorities()).extracting("authority")
                    .containsExactly(TestConstants.RoleConstants.ROLE_USER_NAME);
            assertThat(userDetails.isEnabled()).isTrue();
            assertThat(userDetails.isAccountNonLocked()).isTrue();
        }

        @Test
        @DisplayName("When loading user by username should map disabled and account locked flags if user is not activated and banned")
        void whenUserIsNotActivatedAndBannedShouldMapDisabledAndAccountLockedFlags() {
            User user = UserTestBuilder.firstUser().build();
            user.setRoles(Set.of(RoleTestBuilder.userRole().build()));
            user.setActivated(false);
            user.setBanned(true);

            when(userRepository.findByIgnoreCaseEmail(TestConstants.UserConstants.FIRST_USER_EMAIL))
                    .thenReturn(Optional.of(user));

            UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(TestConstants.UserConstants.FIRST_USER_EMAIL);

            assertThat(userDetails.isEnabled()).isFalse();
            assertThat(userDetails.isAccountNonLocked()).isFalse();
        }
    }
}
