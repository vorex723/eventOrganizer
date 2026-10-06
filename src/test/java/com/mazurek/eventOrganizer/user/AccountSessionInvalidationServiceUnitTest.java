package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.jwt.RefreshTokenService;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountSessionInvalidationService unit tests:")
class AccountSessionInvalidationServiceUnitTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenService refreshTokenService;

    @Test
    void whenInvalidatingSessionsShouldInvalidateAccessAndRefreshCredentialsTogether() {
        User user = UserTestBuilder.firstUser().securityVersion(4L).build();
        AccountSessionInvalidationService service = new AccountSessionInvalidationService(userRepository, refreshTokenService);

        service.invalidateAll(user);

        assertThat(user.getSecurityVersion()).isEqualTo(5L);
        var order = inOrder(userRepository, refreshTokenService);
        order.verify(userRepository).save(user);
        order.verify(refreshTokenService).revokeAllUserTokens(user.getId());
    }
}
