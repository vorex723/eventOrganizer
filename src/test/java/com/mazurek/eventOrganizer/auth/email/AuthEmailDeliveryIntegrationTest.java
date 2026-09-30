package com.mazurek.eventOrganizer.auth.email;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.ActivationToken;
import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.auth.PasswordResetToken;
import com.mazurek.eventOrganizer.auth.PasswordResetTokenRepository;
import com.mazurek.eventOrganizer.auth.EmailChangeToken;
import com.mazurek.eventOrganizer.auth.EmailChangeTokenRepository;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.DEAD;
import static com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.FAILED;
import static com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.SENT;
import static com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.CANCELLED;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.auth.email.worker-enabled=false")
@ActiveProfiles("test")
class AuthEmailDeliveryIntegrationTest {

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
        authEmailSender.configureResult(AuthEmailSendResult.retryableFailure("SMTP unavailable"));

        authEmailDeliveryService.processPendingDeliveries();

        assertThat(authEmailDeliveryRepository.findAll())
                .extracting(AuthEmailDelivery::getStatus)
                .containsExactly(FAILED);

        AuthEmailDelivery failed = authEmailDeliveryRepository.findAll().getFirst();
        failed.setStatus(AuthEmailDeliveryStatus.PENDING);
        failed.setNextAttemptAt(null);
        authEmailDeliveryRepository.saveAndFlush(failed);
        authEmailSender.configureResult(AuthEmailSendResult.permanentFailure("SMTP credentials invalid"));

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
        ActivationToken token = ActivationToken.builder()
                .user(userRepository.findById(userId).orElseThrow())
                .build();
        token.issue(rawToken, 60_000, clock.instant());
        activationTokenRepository.saveAndFlush(token);
        return rawToken;
    }

    private UUID issuePasswordResetToken() {
        UUID rawToken = UUID.randomUUID();
        PasswordResetToken token = PasswordResetToken.builder()
                .user(userRepository.findById(userId).orElseThrow())
                .build();
        token.issue(rawToken, 60_000, clock.instant());
        passwordResetTokenRepository.saveAndFlush(token);
        return rawToken;
    }

    @Test
    void passwordResetDispatchValidationDoesNotAcquireATokenWriteLock() throws Exception {
        UUID token = issuePasswordResetToken();
        authEmailDeliveryService.enqueue(userId, FIRST_USER_EMAIL, AuthEmailType.PASSWORD_RESET, token);
        var executor = Executors.newSingleThreadExecutor();
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
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void cancelsSupersededResetLinkAndSendsOnlyTheCurrentLink() {
        UUID oldToken = issuePasswordResetToken();
        authEmailDeliveryService.enqueue(userId, FIRST_USER_EMAIL, AuthEmailType.PASSWORD_RESET, oldToken);
        PasswordResetToken current = passwordResetTokenRepository.findByUserId(userId).orElseThrow();
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
        EmailChangeToken token = EmailChangeToken.builder().user(userRepository.findById(userId).orElseThrow()).build();
        token.issue(rawToken, "new.address@example.com", 60_000, clock.instant());
        emailChangeTokenRepository.saveAndFlush(token);
        authEmailDeliveryService.enqueue(userId, "wrong.address@example.com", AuthEmailType.EMAIL_CHANGE_CONFIRMATION, rawToken);
        authEmailDeliveryService.enqueue(userId, "NEW.ADDRESS@example.com", AuthEmailType.EMAIL_CHANGE_CONFIRMATION, rawToken);

        authEmailDeliveryService.processPendingDeliveries();

        assertThat(authEmailDeliveryRepository.findAll()).extracting(AuthEmailDelivery::getStatus)
                .containsExactlyInAnyOrder(CANCELLED, SENT);
        assertThat(emailChangeTokenRepository.findByUserId(userId)).isPresent();
        token.issue(rawToken, "new.address@example.com", 0, clock.instant());
        emailChangeTokenRepository.saveAndFlush(token);
        authEmailDeliveryService.enqueue(userId, "new.address@example.com", AuthEmailType.EMAIL_CHANGE_CONFIRMATION, rawToken);

        authEmailDeliveryService.processPendingDeliveries();

        assertThat(authEmailDeliveryRepository.findAll()).extracting(AuthEmailDelivery::getStatus)
                .containsExactlyInAnyOrder(CANCELLED, SENT, CANCELLED);
    }
}
