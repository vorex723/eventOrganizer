package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryService;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Recording email service unit tests:")
class RecordingEmailServiceUnitTest {

    @Nested
    @DisplayName("Recording state tests:")
    class RecordingStateTests {

        private final RecordingEmailService emailService = new RecordingEmailService(
                mock(UserRepository.class), mock(AuthEmailDeliveryService.class));
        private final UUID previousToken = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
        private final UUID currentToken = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");

        @Test
        void whenResettingRecordingsShouldClearAllTokenTypesForEveryRecipient() {
            recordAllTokens(FIRST_USER_EMAIL, previousToken);
            recordAllTokens(SECOND_USER_EMAIL, currentToken);
            assertRecordedTokens(FIRST_USER_EMAIL.toUpperCase(Locale.ROOT), previousToken);
            assertRecordedTokens(SECOND_USER_EMAIL, currentToken);

            emailService.reset();

            assertRecordedTokens(FIRST_USER_EMAIL, null);
            assertRecordedTokens(SECOND_USER_EMAIL.toUpperCase(Locale.ROOT), null);
        }

        @Test
        void whenResettingRepeatedlyShouldAllowFreshRecordingsWithoutRestoringOldRecipients() {
            recordAllTokens(FIRST_USER_EMAIL, previousToken);
            recordAllTokens(SECOND_USER_EMAIL, previousToken);

            emailService.reset();
            emailService.reset();
            recordAllTokens(FIRST_USER_EMAIL.toUpperCase(Locale.ROOT), currentToken);

            assertRecordedTokens(FIRST_USER_EMAIL, currentToken);
            assertRecordedTokens(SECOND_USER_EMAIL, null);
        }

        private void recordAllTokens(String email, UUID token) {
            emailService.sendActivationEmail(email, token);
            emailService.sendPasswordResetEmail(email, token);
            emailService.sendEmailChangeConfirmationEmail(FIRST_USER_ID, email, token);
        }

        private void assertRecordedTokens(String email, UUID token) {
            assertThat(emailService.lastActivationToken(email)).isEqualTo(token);
            assertThat(emailService.lastPasswordResetToken(email)).isEqualTo(token);
            assertThat(emailService.lastEmailChangeToken(email)).isEqualTo(token);
        }
    }
}
