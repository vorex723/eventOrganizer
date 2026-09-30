package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthUserLockServiceUnitTest {
    @Mock private UserRepository userRepository;
    @Mock private EntityManager entityManager;

    @Test
    void refreshesManagedUserOnlyAfterAcquiringTheRowLock() {
        User user = UserTestBuilder.firstUser().build();
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));

        assertThat(service().lockById(user.getId())).contains(user);

        var order = inOrder(userRepository, entityManager);
        order.verify(userRepository).findByIdForUpdate(user.getId());
        order.verify(entityManager).refresh(user);
    }

    @Test
    void missingUserDoesNotAttemptARefresh() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());
        assertThat(service().lockById(id)).isEmpty();
        verifyNoInteractions(entityManager);
    }

    @Test
    void emailLookupAcceptsCaseDifferencesAfterRefresh() {
        User user = UserTestBuilder.firstUser().build();
        String email = user.getEmail().toUpperCase(java.util.Locale.ROOT);
        when(userRepository.findByIgnoreCaseEmailForUpdate(email)).thenReturn(Optional.of(user));
        assertThat(service().lockByEmail(email)).contains(user);
        verify(entityManager).refresh(user);
    }

    @Test
    void emailLookupRejectsAnAddressChangedWhileWaitingForTheLock() {
        User user = UserTestBuilder.firstUser().build();
        String oldEmail = user.getEmail();
        when(userRepository.findByIgnoreCaseEmailForUpdate(oldEmail)).thenReturn(Optional.of(user));
        doAnswer(ignored -> {
            user.setEmail("updated@example.com");
            return null;
        }).when(entityManager).refresh(user);

        assertThat(service().lockByEmail(oldEmail)).isEmpty();
    }

    private AuthUserLockService service() {
        return new AuthUserLockService(userRepository, entityManager);
    }
}
