package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.testData.builders.ResetPasswordRequestTestBuilder;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.dto.ResetPasswordRequest;
import com.mazurek.eventOrganizer.exception.auth.PasswordResetTokenNotFoundException;
import com.mazurek.eventOrganizer.notification.service.RecordingEmailService;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestPersistenceQueries;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import com.mazurek.eventOrganizer.testSupport.concurrency.TestWorkers;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("PasswordResetToken concurrency integration tests:")
class PasswordResetTokenConcurrencyIntegrationTest {

    @Autowired
    private TestPersistenceQueries testPersistenceQueries;

    @Autowired private DeletionService deletionService;
    @Autowired private AuthHelper authHelper;
    @Autowired private AuthenticationService authenticationService;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RecordingEmailService emailService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private AuthUserLockService authUserLockService;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
    }

    @AfterEach
    void tearDown() {
        deletionService.deleteAllSafe();
    }

    @Test
    void competingResetWaitsUntilTokenIsConsumedAndThenFails() throws Exception {
        User user = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected persisted user in competingResetWaitsUntilTokenIsConsumedAndThenFails");
        long previousSecurityVersion = user.getSecurityVersion();
        authenticationService.requestPasswordReset(user.getEmail());
        UUID token = emailService.lastPasswordResetToken(user.getEmail());
        assertThat(token).isNotNull();

        ResetPasswordRequest request = new ResetPasswordRequestTestBuilder().password(UserConstants.NEW_PASSWORD)
                .passwordConfirmation(UserConstants.NEW_PASSWORD).build();
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = TestWorkers.newSingleThreadExecutor();
        try {
            Future<Boolean> competingReset = executor.submit(() -> resetAfterStart(token, request, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS))
                    .as("Competing password reset reached the start barrier").isTrue();
            transactionTemplate.executeWithoutResult(ignored -> {
                assertThat(authUserLockService.lockById(user.getId())).isPresent();
                assertThat(passwordResetTokenRepository.findByToken(token)).isPresent();
                start.countDown();
                assertThatThrownBy(() -> competingReset.get(1, TimeUnit.SECONDS))
                        .isInstanceOf(TimeoutException.class);
                authenticationService.resetPassword(token, request);
            });
            assertThat(competingReset.get(10, TimeUnit.SECONDS)).isFalse();
        } finally {
            start.countDown();
            TestWorkers.stop(executor);
        }

        User updatedUser = requirePresent(userRepository.findById(user.getId()), "Expected persisted user in competingResetWaitsUntilTokenIsConsumedAndThenFails");
        assertThat(testPersistenceQueries.findPasswordResetTokenByUserId(user.getId())).isEmpty();
        assertThat(passwordEncoder.matches(UserConstants.NEW_PASSWORD, updatedUser.getPassword())).isTrue();
        assertThat(updatedUser.getSecurityVersion()).isEqualTo(previousSecurityVersion + 1);
    }

    private boolean resetAfterStart(UUID token, ResetPasswordRequest request,
                                    CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        assertThat(start.await(10, TimeUnit.SECONDS))
                .as("Password reset worker received the start signal").isTrue();
        try {
            authenticationService.resetPassword(token, request);
            return true;
        } catch (PasswordResetTokenNotFoundException exception) {
            return false;
        }
    }
}
