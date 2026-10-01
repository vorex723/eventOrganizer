package com.mazurek.eventOrganizer.auth;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
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
        User user = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow();
        long previousSecurityVersion = user.getSecurityVersion();
        authenticationService.requestPasswordReset(user.getEmail());
        UUID token = emailService.lastPasswordResetToken(user.getEmail());
        assertThat(token).isNotNull();

        ResetPasswordRequest request = new ResetPasswordRequest(
                UserConstants.NEW_PASSWORD, UserConstants.NEW_PASSWORD);
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> competingReset = executor.submit(() -> resetAfterStart(token, request, ready, start));
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
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
            executor.shutdownNow();
        }

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(testPersistenceQueries.findPasswordResetTokenByUserId(user.getId())).isEmpty();
        assertThat(passwordEncoder.matches(UserConstants.NEW_PASSWORD, updatedUser.getPassword())).isTrue();
        assertThat(updatedUser.getSecurityVersion()).isEqualTo(previousSecurityVersion + 1);
    }

    private boolean resetAfterStart(UUID token, ResetPasswordRequest request,
                                    CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            authenticationService.resetPassword(token, request);
            return true;
        } catch (PasswordResetTokenNotFoundException exception) {
            return false;
        }
    }
}
