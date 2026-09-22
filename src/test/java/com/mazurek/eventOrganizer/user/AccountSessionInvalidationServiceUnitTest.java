package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.jwt.RefreshTokenService;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountSessionInvalidationServiceUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenService refreshTokenService;

    @Test
    void invalidatesAccessAndRefreshCredentialsTogether() {
        User user = UserTestBuilder.firstUser().securityVersion(4L).build();
        AccountSessionInvalidationService service = new AccountSessionInvalidationService(userRepository, refreshTokenService);

        service.invalidateAll(user);

        assertThat(user.getSecurityVersion()).isEqualTo(5L);
        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllUserTokens(user.getId());
    }
}
