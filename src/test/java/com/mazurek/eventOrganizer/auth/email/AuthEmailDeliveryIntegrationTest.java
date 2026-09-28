package com.mazurek.eventOrganizer.auth.email;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.ActivationToken;
import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.auth.PasswordResetToken;
import com.mazurek.eventOrganizer.auth.PasswordResetTokenRepository;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.time.Instant;

import static com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.DEAD;
import static com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.FAILED;
import static com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.SENT;
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
    private AuthEmailSenderTestImpl authEmailSender;
    @Autowired
    private ActivationTokenRepository activationTokenRepository;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
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
        token.issue(rawToken, 60_000, Instant.now());
        activationTokenRepository.saveAndFlush(token);
        return rawToken;
    }

    private UUID issuePasswordResetToken() {
        UUID rawToken = UUID.randomUUID();
        PasswordResetToken token = PasswordResetToken.builder()
                .user(userRepository.findById(userId).orElseThrow())
                .build();
        token.issue(rawToken, 60_000, Instant.now());
        passwordResetTokenRepository.saveAndFlush(token);
        return rawToken;
    }
}
