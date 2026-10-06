package com.mazurek.eventOrganizer.auth.email;

import com.mazurek.eventOrganizer.testData.builders.AuthEmailSendResultTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ActivationTokenTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.PasswordResetTokenTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.EmailChangeTokenTestBuilder;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.ActivationToken;
import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.auth.PasswordResetToken;
import com.mazurek.eventOrganizer.auth.PasswordResetTokenRepository;
import com.mazurek.eventOrganizer.auth.EmailChangeToken;
import com.mazurek.eventOrganizer.auth.EmailChangeTokenRepository;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestPersistenceQueries;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.time.Clock;
import com.mazurek.eventOrganizer.testSupport.concurrency.TestWorkers;
import java.util.concurrent.TimeUnit;

import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.DEAD;
import static com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.FAILED;
import static com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.SENT;
import static com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.CANCELLED;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.auth.email.worker-enabled=false")
@ActiveProfiles("test")
@DisplayName("AuthEmailDelivery integration tests:")
class AuthEmailDeliveryIntegrationTest {

    @Autowired
    private TestPersistenceQueries testPersistenceQueries;

    @Autowired
    private AuthEmailDeliveryService authEmailDeliveryService;
    @Autowired
    private AuthEmailDeliveryRepository authEmailDeliveryRepository;
    @Autowired
    private TestAuthEmailSender authEmailSender;
    @Autowired
    private ActivationTokenRepository activationTokenRepository;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired private EmailChangeTokenRepository emailChangeTokenRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private Clock clock;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private DeletionService deletionService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        authEmailDeliveryRepository.deleteAll();
        authEmailSender.reset();
        userId = userRepository.findByIgnoreCaseEmail(FIRST_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new)
                .getId();
    }

    @AfterEach
    void tearDown() {
        TestWorkers.requireStopped();
        authEmailSender.reset();
        deletionService.deleteAllSafe();
    }

    @Test
    void sendsPendingAuthEmailAfterItWasCommitted() {
        UUID token = issueActivationToken();
        authEmailDeliveryService.enqueue(
                userId,
                FIRST_USER_EMAIL,
                AuthEmailType.ACCOUNT_ACTIVATION,
                token
        );

        authEmailDeliveryService.processPendingDeliveries();

        assertThat(authEmailDeliveryRepository.findAll())
                .extracting(AuthEmailDelivery::getStatus)
                .containsExactly(SENT);
    }

    @Test
    void retriesTemporarySmtpFailureAndMarksPermanentFailureDead() {
        UUID token = issuePasswordResetToken();
        authEmailDeliveryService.enqueue(
                userId,
                FIRST_USER_EMAIL,
                AuthEmailType.PASSWORD_RESET,
                token
        );
        authEmailSender.configureResult(new AuthEmailSendResultTestBuilder()
                .outcome(AuthEmailSendOutcome.RETRYABLE_FAILURE)
                .providerMessageId(null)
                .errorMessage("SMTP unavailable")
                .build());

        authEmailDeliveryService.processPendingDeliveries();

        assertThat(authEmailDeliveryRepository.findAll())
                .extracting(AuthEmailDelivery::getStatus)
                .containsExactly(FAILED);

        AuthEmailDelivery failed = authEmailDeliveryRepository.findAll().getFirst();
        failed.setStatus(AuthEmailDeliveryStatus.PENDING);
        failed.setNextAttemptAt(null);
        authEmailDeliveryRepository.saveAndFlush(failed);
        authEmailSender.configureResult(new AuthEmailSendResultTestBuilder()
                .outcome(AuthEmailSendOutcome.PERMANENT_FAILURE)
                .providerMessageId(null)
                .errorMessage("SMTP credentials invalid")
                .build());

        authEmailDeliveryService.processPendingDeliveries();

        assertThat(authEmailDeliveryRepository.findAll())
                .extracting(AuthEmailDelivery::getStatus)
                .containsExactly(DEAD);
    }

    @Test
    void marksUnreadableOutboxPayloadDeadInsteadOfLeavingItProcessing() {
        UUID token = issueActivationToken();
        authEmailDeliveryService.enqueue(
                userId,
                FIRST_USER_EMAIL,
                AuthEmailType.ACCOUNT_ACTIVATION,
                token
        );
        AuthEmailDelivery delivery = authEmailDeliveryRepository.findAll().getFirst();
        delivery.setEncryptedToken("not-an-encrypted-token");
        authEmailDeliveryRepository.saveAndFlush(delivery);

        authEmailDeliveryService.processPendingDeliveries();

        assertThat(authEmailDeliveryRepository.findAll())
                .extracting(AuthEmailDelivery::getStatus)
                .containsExactly(DEAD);
    }

    private UUID issueActivationToken() {
        UUID rawToken = UUID.randomUUID();
        ActivationToken token = new ActivationTokenTestBuilder().id(null).unissued()
                .user(requirePresent(userRepository.findById(userId), "Expected persisted user in issueActivationToken"))
                .build();
        token.issue(rawToken, 60_000, clock.instant());
        activationTokenRepository.saveAndFlush(token);
        return rawToken;
    }

    private UUID issuePasswordResetToken() {
        UUID rawToken = UUID.randomUUID();
        PasswordResetToken token = new PasswordResetTokenTestBuilder().id(null).unissued()
                .user(requirePresent(userRepository.findById(userId), "Expected persisted user in issuePasswordResetToken"))
                .build();
        token.issue(rawToken, 60_000, clock.instant());
        passwordResetTokenRepository.saveAndFlush(token);
        return rawToken;
    }

    @Test
    void activationDispatchValidationDoesNotAcquireTokenOrUserWriteLocks() throws Exception {
        UUID token = issueActivationToken();
        authEmailDeliveryService.enqueue(userId, FIRST_USER_EMAIL, AuthEmailType.ACCOUNT_ACTIVATION, token);
        var executor = TestWorkers.newSingleThreadExecutor();
        try {
            transactionTemplate.executeWithoutResult(ignored -> {
                requirePresent(userRepository.findByIdForUpdate(userId), "Expected persisted user in activationDispatchValidationDoesNotAcquireTokenOrUserWriteLocks");
                requirePresent(activationTokenRepository.findByToken(token), "Expected activation token in activationDispatchValidationDoesNotAcquireTokenOrUserWriteLocks");
                var dispatch = executor.submit(authEmailDeliveryService::processPendingDeliveries);
                try {
                    dispatch.get(5, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            });
            assertThat(authEmailDeliveryRepository.findAll()).extracting(AuthEmailDelivery::getStatus)
                    .containsExactly(SENT);
            assertThat(testPersistenceQueries.findActivationToken(token)).isPresent();
        } finally {
            TestWorkers.stop(executor);
        }
    }

    @Test
    void activationDispatchChecksHashUserAndExpirationWithoutConsumingToken() {
        UUID oldToken = issueActivationToken();
        authEmailDeliveryService.enqueue(userId, FIRST_USER_EMAIL, AuthEmailType.ACCOUNT_ACTIVATION, oldToken);
        ActivationToken current = requirePresent(testPersistenceQueries.findActivationToken(oldToken), "Expected activation token in activationDispatchChecksHashUserAndExpirationWithoutConsumingToken");
        UUID newToken = UUID.randomUUID();
        current.issue(newToken, 60_000, clock.instant());
        activationTokenRepository.saveAndFlush(current);
        authEmailDeliveryService.enqueue(userId, FIRST_USER_EMAIL, AuthEmailType.ACCOUNT_ACTIVATION, newToken);
        UUID otherUserId = requirePresent(userRepository.findAll().stream().filter(user -> !user.getId().equals(userId))
                .findFirst(), "Expected persisted user in activationDispatchChecksHashUserAndExpirationWithoutConsumingToken").getId();
        authEmailDeliveryService.enqueue(otherUserId, FIRST_USER_EMAIL, AuthEmailType.ACCOUNT_ACTIVATION, newToken);

        authEmailDeliveryService.processPendingDeliveries();

        assertThat(authEmailDeliveryRepository.findAll()).extracting(AuthEmailDelivery::getStatus)
                .containsExactlyInAnyOrder(CANCELLED, SENT, CANCELLED);
        assertThat(testPersistenceQueries.findActivationToken(newToken)).isPresent();
        current.issue(newToken, 0, clock.instant());
        activationTokenRepository.saveAndFlush(current);
        authEmailDeliveryService.enqueue(userId, FIRST_USER_EMAIL, AuthEmailType.ACCOUNT_ACTIVATION, newToken);

        authEmailDeliveryService.processPendingDeliveries();

        assertThat(authEmailDeliveryRepository.findAll()).extracting(AuthEmailDelivery::getStatus)
                .containsExactlyInAnyOrder(CANCELLED, SENT, CANCELLED, CANCELLED);
    }

    @Test
    void passwordResetDispatchValidationDoesNotAcquireATokenWriteLock() throws Exception {
        UUID token = issuePasswordResetToken();
        authEmailDeliveryService.enqueue(userId, FIRST_USER_EMAIL, AuthEmailType.PASSWORD_RESET, token);
        var executor = TestWorkers.newSingleThreadExecutor();
        try {
            transactionTemplate.executeWithoutResult(ignored -> {
                assertThat(passwordResetTokenRepository.findByToken(token)).isPresent();
                var dispatch = executor.submit(authEmailDeliveryService::processPendingDeliveries);
                try {
                    dispatch.get(5, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            });
            assertThat(authEmailDeliveryRepository.findAll()).extracting(AuthEmailDelivery::getStatus)
                    .containsExactly(SENT);
        } finally {
            TestWorkers.stop(executor);
        }
    }

    @Test
    void cancelsSupersededResetLinkAndSendsOnlyTheCurrentLink() {
        UUID oldToken = issuePasswordResetToken();
        authEmailDeliveryService.enqueue(userId, FIRST_USER_EMAIL, AuthEmailType.PASSWORD_RESET, oldToken);
        PasswordResetToken current = requirePresent(testPersistenceQueries.findPasswordResetTokenByUserId(userId), "Expected password reset token in cancelsSupersededResetLinkAndSendsOnlyTheCurrentLink");
        UUID newToken = UUID.randomUUID();
        current.issue(newToken, 60_000, clock.instant());
        passwordResetTokenRepository.saveAndFlush(current);
        authEmailDeliveryService.enqueue(userId, FIRST_USER_EMAIL, AuthEmailType.PASSWORD_RESET, newToken);

        authEmailDeliveryService.processPendingDeliveries();

        assertThat(authEmailDeliveryRepository.findAll()).extracting(AuthEmailDelivery::getStatus)
                .containsExactlyInAnyOrder(CANCELLED, SENT);
    }

    @Test
    void emailChangeDispatchChecksRecipientAndExpirationWithoutConsumingToken() {
        UUID rawToken = UUID.randomUUID();
        EmailChangeToken token = new EmailChangeTokenTestBuilder().id(null).unissued()
                .user(requirePresent(userRepository.findById(userId), "Expected persisted user in emailChangeDispatchChecksRecipientAndExpirationWithoutConsumingToken")).build();
        token.issue(rawToken, "new.address@example.com", 60_000, clock.instant());
        emailChangeTokenRepository.saveAndFlush(token);
        authEmailDeliveryService.enqueue(userId, "wrong.address@example.com", AuthEmailType.EMAIL_CHANGE_CONFIRMATION, rawToken);
        authEmailDeliveryService.enqueue(userId, "NEW.ADDRESS@example.com", AuthEmailType.EMAIL_CHANGE_CONFIRMATION, rawToken);

        authEmailDeliveryService.processPendingDeliveries();

        assertThat(authEmailDeliveryRepository.findAll()).extracting(AuthEmailDelivery::getStatus)
                .containsExactlyInAnyOrder(CANCELLED, SENT);
        assertThat(testPersistenceQueries.findEmailChangeTokenByUserId(userId)).isPresent();
        token.issue(rawToken, "new.address@example.com", 0, clock.instant());
        emailChangeTokenRepository.saveAndFlush(token);
        authEmailDeliveryService.enqueue(userId, "new.address@example.com", AuthEmailType.EMAIL_CHANGE_CONFIRMATION, rawToken);

        authEmailDeliveryService.processPendingDeliveries();

        assertThat(authEmailDeliveryRepository.findAll()).extracting(AuthEmailDelivery::getStatus)
                .containsExactlyInAnyOrder(CANCELLED, SENT, CANCELLED);
    }
}
