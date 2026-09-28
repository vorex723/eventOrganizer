package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.auth.email.AuthEmailType;
import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import com.mazurek.eventOrganizer.notification.service.EmailService;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.AccountSessionInvalidationService;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailChangeServiceUnitTest {
    private static final Instant NOW = Instant.parse("2026-09-23T10:00:00Z");

    @Mock private EmailChangeTokenRepository emailChangeTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private AccountSessionInvalidationService accountSessionInvalidationService;
    @Mock private EmailService emailService;

    private EmailChangeService service() {
        AuthProperties properties = new AuthProperties();
        properties.setEmailChangeTokenExpiration(60_000);
        return new EmailChangeService(emailChangeTokenRepository, passwordResetTokenRepository, userRepository,
                accountSessionInvalidationService, emailService, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void requestKeepsCurrentEmailAndQueuesConfirmationForNormalizedPendingAddress() {
        User user = UserTestBuilder.firstUser().build();
        when(emailChangeTokenRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        service().requestChange(user, "New.Address@Example.COM");

        ArgumentCaptor<EmailChangeToken> token = ArgumentCaptor.forClass(EmailChangeToken.class);
        verify(emailChangeTokenRepository).save(token.capture());
        assertThat(user.getEmail()).isEqualTo("first.user@example.com");
        assertThat(token.getValue().getPendingEmail()).isEqualTo("new.address@example.com");
        verify(emailService).cancelPendingEmails(user.getId(), AuthEmailType.EMAIL_CHANGE_CONFIRMATION);
        verify(emailService).sendEmailChangeConfirmationEmail(eq(user.getId()), eq("new.address@example.com"), any(UUID.class));
        verifyNoInteractions(accountSessionInvalidationService);
    }

    @Test
    void confirmationChangesEmailAndInvalidatesExistingSessions() {
        User user = UserTestBuilder.firstUser().build();
        UUID rawToken = UUID.randomUUID();
        EmailChangeToken token = EmailChangeToken.builder().user(user).build();
        token.issue(rawToken, "new.address@example.com", 60_000, NOW);
        when(emailChangeTokenRepository.findByTokenHash(AuthTokenHash.sha256(rawToken))).thenReturn(Optional.of(token));
        when(userRepository.findByIgnoreCaseEmail("new.address@example.com")).thenReturn(Optional.empty());
        when(passwordResetTokenRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        EmailChangeResult result = service().confirmChange(rawToken);

        assertThat(result).isEqualTo(EmailChangeResult.CHANGED);
        assertThat(user.getEmail()).isEqualTo("new.address@example.com");
        verify(accountSessionInvalidationService).invalidateAll(user);
        verify(emailChangeTokenRepository).delete(token);
        verify(emailService).cancelPendingEmails(user.getId(), AuthEmailType.PASSWORD_RESET);
    }

    @Test
    void expiredConfirmationDeletesThePendingChangeWithoutChangingTheEmail() {
        User user = UserTestBuilder.firstUser().build();
        UUID rawToken = UUID.randomUUID();
        EmailChangeToken token = EmailChangeToken.builder().user(user).build();
        token.issue(rawToken, "new.address@example.com", 0, NOW);
        when(emailChangeTokenRepository.findByTokenHash(AuthTokenHash.sha256(rawToken))).thenReturn(Optional.of(token));

        assertThat(service().confirmChange(rawToken)).isEqualTo(EmailChangeResult.EXPIRED);
        assertThat(user.getEmail()).isEqualTo("first.user@example.com");
        verify(emailChangeTokenRepository).delete(token);
        verify(emailService).cancelPendingEmails(user.getId(), AuthEmailType.EMAIL_CHANGE_CONFIRMATION);
        verifyNoInteractions(accountSessionInvalidationService);
    }

    @Test
    void confirmationReportsUnavailableWhenAnotherUserOwnsTheRequestedAddress() {
        User user = UserTestBuilder.firstUser().build();
        User addressOwner = UserTestBuilder.secondUser().build();
        UUID rawToken = UUID.randomUUID();
        EmailChangeToken token = EmailChangeToken.builder().user(user).build();
        token.issue(rawToken, "new.address@example.com", 60_000, NOW);
        when(emailChangeTokenRepository.findByTokenHash(AuthTokenHash.sha256(rawToken))).thenReturn(Optional.of(token));
        when(userRepository.findByIgnoreCaseEmail("new.address@example.com")).thenReturn(Optional.of(addressOwner));

        assertThat(service().confirmChange(rawToken)).isEqualTo(EmailChangeResult.EMAIL_UNAVAILABLE);
        assertThat(user.getEmail()).isEqualTo("first.user@example.com");
        verify(emailChangeTokenRepository, never()).delete(token);
        verifyNoInteractions(accountSessionInvalidationService);
    }

    @Test
    void aConsumedConfirmationTokenCannotBeReplayed() {
        User user = UserTestBuilder.firstUser().build();
        UUID rawToken = UUID.randomUUID();
        EmailChangeToken token = EmailChangeToken.builder().user(user).build();
        token.issue(rawToken, "new.address@example.com", 60_000, NOW);
        when(emailChangeTokenRepository.findByTokenHash(AuthTokenHash.sha256(rawToken)))
                .thenReturn(Optional.of(token), Optional.empty());
        when(userRepository.findByIgnoreCaseEmail("new.address@example.com")).thenReturn(Optional.empty());
        when(passwordResetTokenRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        assertThat(service().confirmChange(rawToken)).isEqualTo(EmailChangeResult.CHANGED);
        assertThat(service().confirmChange(rawToken)).isEqualTo(EmailChangeResult.INVALID_TOKEN);
        verify(accountSessionInvalidationService, times(1)).invalidateAll(user);
    }
}
