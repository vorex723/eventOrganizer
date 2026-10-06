package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.testData.builders.ActivationTokenTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.PasswordResetTokenTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ResetPasswordRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RefreshTokenRequestTestBuilder;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.dto.ResetPasswordRequest;
import com.mazurek.eventOrganizer.auth.email.AuthEmailDelivery;
import com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryRepository;
import com.mazurek.eventOrganizer.auth.email.AuthEmailMaintenanceService;
import com.mazurek.eventOrganizer.auth.email.AuthEmailType;
import com.mazurek.eventOrganizer.exception.auth.PasswordResetTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.auth.ActivationTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenRevokedException;
import com.mazurek.eventOrganizer.exception.user.InvalidPasswordException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.jwt.RefreshTokenRepository;
import com.mazurek.eventOrganizer.jwt.RefreshTokenService;
import com.mazurek.eventOrganizer.notification.service.RecordingEmailService;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestPersistenceQueries;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserEmailDtoTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.user.UserService;
import com.mazurek.eventOrganizer.utils.EncryptionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.IllegalTransactionStateException;

import java.time.Duration;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import com.mazurek.eventOrganizer.testSupport.concurrency.TestWorkers;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.CANCELLED;
import static com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryStatus.PENDING;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=4",
        "app.auth.email.worker-enabled=false"
})
@ActiveProfiles("test")
@DisplayName("Auth token issuance concurrency tests:")
class AuthTokenIssuanceConcurrencyIntegrationTest {

    @Autowired
    private TestPersistenceQueries testPersistenceQueries;
    @Autowired private DeletionService deletionService;
    @Autowired private AuthHelper authHelper;
    @Autowired private AuthenticationService authenticationService;
    @Autowired private EmailChangeService emailChangeService;
    @Autowired private UserService userService;
    @Autowired private AuthUserLockService authUserLockService;
    @Autowired private UserRepository userRepository;
    @Autowired private ActivationTokenRepository activationTokenRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private EmailChangeTokenRepository emailChangeTokenRepository;
    @Autowired private AuthEmailDeliveryRepository deliveryRepository;
    @Autowired private AuthEmailMaintenanceService maintenanceService;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private RecordingEmailService emailService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EncryptionUtils encryptionUtils;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private Clock clock;

    private User user;

    @BeforeEach
    void setUp() {
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        user = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected persisted user in setUp");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Test
    void concurrentActivationConsumesAValidTokenOnlyOnce() throws Exception {
        UUID token = issueActivationToken(false);

        RaceResult<ActivationResult> result = raceWithBlockedSecond(
                () -> authenticationService.activateAccount(token),
                () -> authenticationService.activateAccount(token), ActivationTokenNotFoundException.class);

        assertThat(result.first()).isEqualTo(ActivationResult.ACTIVATED);
        assertThat(result.second()).isNull();
        assertThat(requirePresent(userRepository.findById(user.getId()), "Expected persisted user in concurrentActivationConsumesAValidTokenOnlyOnce").isActivated()).isTrue();
        assertThat(testPersistenceQueries.findActivationToken(token)).isEmpty();
        assertThat(deliveries(AuthEmailType.ACCOUNT_ACTIVATION)).hasSize(1)
                .allMatch(delivery -> delivery.getStatus() == CANCELLED);
    }

    @Test
    void concurrentExpiredActivationRegeneratesOnceAndQueuesOneCurrentLink() throws Exception {
        UUID token = issueActivationToken(true);

        RaceResult<ActivationResult> result = raceWithBlockedSecond(
                () -> authenticationService.activateAccount(token),
                () -> authenticationService.activateAccount(token), ActivationTokenNotFoundException.class);

        assertThat(result.first()).isEqualTo(ActivationResult.TOKEN_EXPIRED_NEW_SENT);
        assertThat(result.second()).isNull();
        assertThat(testPersistenceQueries.findActivationToken(token)).isEmpty();
        assertUnactivatedWithOneCurrentActivationDelivery();
    }

    @Test
    void concurrentActivationResendsRenewAnExistingTokenOnlyOnce() throws Exception {
        UUID token = issueActivationToken(false);
        ActivationToken previous = requirePresent(testPersistenceQueries.findActivationToken(token), "Expected activation token in concurrentActivationResendsRenewAnExistingTokenOnlyOnce");
        ageDeliveries(AuthEmailType.ACCOUNT_ACTIVATION);

        raceWithBlockedSecond(this::requestActivationResend, this::requestActivationResend);

        ActivationToken current = requirePresent(testPersistenceQueries.findActivationTokenByUserEmail(user.getEmail()), "Expected activation token in concurrentActivationResendsRenewAnExistingTokenOnlyOnce");
        assertThat(current.getId()).isEqualTo(previous.getId());
        assertThat(current.getTokenHash()).isNotEqualTo(previous.getTokenHash());
        assertUnactivatedWithOneCurrentActivationDelivery();
    }

    @Test
    void concurrentActivationResendsAfterCleanupCreateOneTokenWithoutUniqueConflict() throws Exception {
        issueActivationToken(true);
        ageDeliveries(AuthEmailType.ACCOUNT_ACTIVATION);
        maintenanceService.cleanup();
        assertThat(testPersistenceQueries.findActivationTokenByUserEmail(user.getEmail())).isEmpty();

        raceWithBlockedSecond(this::requestActivationResend, this::requestActivationResend);

        assertUnactivatedWithOneCurrentActivationDelivery();
    }

    @Test
    void activationBeforeResendRefreshesTheWaitingRequestsUserStateAndDoesNotIssueAnotherToken() throws Exception {
        UUID token = issueActivationToken(false);
        ageDeliveries(AuthEmailType.ACCOUNT_ACTIVATION);

        raceWithBlockedSecond(() -> {
            assertThat(authenticationService.activateAccount(token)).isEqualTo(ActivationResult.ACTIVATED);
            return true;
        }, this::requestActivationResend);

        assertThat(requirePresent(userRepository.findById(user.getId()), "Expected persisted user in activationBeforeResendRefreshesTheWaitingRequestsUserStateAndDoesNotIssueAnotherToken").isActivated()).isTrue();
        assertThat(testPersistenceQueries.findActivationTokenByUserEmail(user.getEmail())).isEmpty();
        assertThat(deliveries(AuthEmailType.ACCOUNT_ACTIVATION)).hasSize(1)
                .allMatch(delivery -> delivery.getStatus() == CANCELLED);
    }

    @Test
    void resendBeforeActivationRejectsTheReplacedTokenWithoutActivatingTheAccount() throws Exception {
        UUID token = issueActivationToken(false);
        ageDeliveries(AuthEmailType.ACCOUNT_ACTIVATION);

        raceWithBlockedSecond(this::requestActivationResend, () -> {
            authenticationService.activateAccount(token);
            return true;
        }, ActivationTokenNotFoundException.class);

        assertThat(testPersistenceQueries.findActivationToken(token)).isEmpty();
        assertUnactivatedWithOneCurrentActivationDelivery();
    }

    @Test
    void cleanupBeforeExpiredActivationReturnsInvalidTokenWithoutIssuingAnotherLink() throws Exception {
        UUID token = issueActivationToken(true);

        raceWithBlockedSecond(
                () -> requirePresent(activationTokenRepository.findByUserIdForUpdate(user.getId()), "Expected activation token in cleanupBeforeExpiredActivationReturnsInvalidTokenWithoutIssuingAnotherLink"),
                () -> {
                    maintenanceService.cleanup();
                    return null;
                }, () -> authenticationService.activateAccount(token), ActivationTokenNotFoundException.class);

        assertThat(testPersistenceQueries.findActivationTokenByUserEmail(user.getEmail())).isEmpty();
        assertThat(requirePresent(userRepository.findById(user.getId()), "Expected persisted user in cleanupBeforeExpiredActivationReturnsInvalidTokenWithoutIssuingAnotherLink").isActivated()).isFalse();
        assertThat(deliveries(AuthEmailType.ACCOUNT_ACTIVATION)).hasSize(1);
    }

    @Test
    void expiredActivationBeforeCleanupPreservesTheRenewedTokenAndItsCurrentLink() throws Exception {
        UUID token = issueActivationToken(true);

        RaceResult<ActivationResult> result = raceWithBlockedSecond(() -> {
            requirePresent(authUserLockService.lockById(user.getId()), "Expected locked user in expiredActivationBeforeCleanupPreservesTheRenewedTokenAndItsCurrentLink");
            requirePresent(activationTokenRepository.findByUserIdForUpdate(user.getId()), "Expected activation token in expiredActivationBeforeCleanupPreservesTheRenewedTokenAndItsCurrentLink");
        }, () -> authenticationService.activateAccount(token), () -> {
            maintenanceService.cleanup();
            return null;
        });

        assertThat(result.first()).isEqualTo(ActivationResult.TOKEN_EXPIRED_NEW_SENT);
        assertUnactivatedWithOneCurrentActivationDelivery();
    }

    @Test
    void cleanupBeforeResendAllowsRecreationOfTheDeletedActivationTokenWithoutDeadlock() throws Exception {
        issueActivationToken(true);
        ageDeliveries(AuthEmailType.ACCOUNT_ACTIVATION);

        raceWithBlockedSecond(
                () -> requirePresent(activationTokenRepository.findByUserIdForUpdate(user.getId()), "Expected activation token in cleanupBeforeResendAllowsRecreationOfTheDeletedActivationTokenWithoutDeadlock"),
                () -> {
                    maintenanceService.cleanup();
                    return null;
                }, this::requestActivationResend);

        assertUnactivatedWithOneCurrentActivationDelivery();
    }

    @Test
    void lockingOneUserDoesNotBlockActivationResendForAnotherUser() throws Exception {
        prepareUnactivatedUser();
        User other = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL), "Expected persisted user in lockingOneUserDoesNotBlockActivationResendForAnotherUser");
        other.setActivated(false);
        userRepository.saveAndFlush(other);
        ExecutorService executor = TestWorkers.newSingleThreadExecutor();
        try {
            transactionTemplate.executeWithoutResult(ignored -> {
                requirePresent(authUserLockService.lockById(user.getId()), "Expected locked user in lockingOneUserDoesNotBlockActivationResendForAnotherUser");
                Future<?> otherRequest = executor.submit(() ->
                        authenticationService.regenerateActivationTokenByUserEmail(other.getEmail()));
                try {
                    otherRequest.get(5, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            });
            assertThat(testPersistenceQueries.findActivationTokenByUserEmail(other.getEmail())).isPresent();
            assertThat(testPersistenceQueries.findActivationTokenByUserEmail(user.getEmail())).isEmpty();
            assertThat(deliveryRepository.findAll()).hasSize(1)
                    .allMatch(delivery -> delivery.getUserId().equals(other.getId()) && delivery.getStatus() == PENDING);
        } finally {
            TestWorkers.stop(executor);
        }
    }

    private void prepareUnactivatedUser() {
        user.setActivated(false);
        userRepository.saveAndFlush(user);
        deliveryRepository.deleteAll();
    }

    private UUID issueActivationToken(boolean expired) {
        prepareUnactivatedUser();
        UUID rawToken = UUID.randomUUID();
        ActivationToken token = new ActivationTokenTestBuilder().id(null).user(user).unissued().build();
        token.issue(rawToken, expired ? 0 : 60_000, clock.instant().minusSeconds(1));
        activationTokenRepository.saveAndFlush(token);
        emailService.sendActivationEmail(user.getEmail(), rawToken);
        return rawToken;
    }

    private Boolean requestActivationResend() {
        authenticationService.regenerateActivationTokenByUserEmail(user.getEmail());
        return true;
    }

    private void assertUnactivatedWithOneCurrentActivationDelivery() {
        assertThat(requirePresent(userRepository.findById(user.getId()), "Expected persisted user in assertUnactivatedWithOneCurrentActivationDelivery").isActivated()).isFalse();
        assertThat(activationTokenRepository.count()).isEqualTo(1);
        ActivationToken current = requirePresent(testPersistenceQueries.findActivationTokenByUserEmail(user.getEmail()), "Expected activation token in assertUnactivatedWithOneCurrentActivationDelivery");
        assertThat(current.isExpired(clock.instant())).isFalse();
        assertOneCurrentDelivery(AuthEmailType.ACCOUNT_ACTIVATION, current.getTokenHash());
        assertThat(deliveries(AuthEmailType.ACCOUNT_ACTIVATION)).hasSize(2)
                .filteredOn(delivery -> delivery.getStatus() == CANCELLED).hasSize(1);
    }

    @Test
    void concurrentFirstPasswordResetRequestsCreateOneTokenAndOneDelivery() throws Exception {
        raceWithBlockedSecond(() -> requestReset(), () -> requestReset());

        assertThat(passwordResetTokenRepository.findAll()).hasSize(1);
        assertOneCurrentDelivery(AuthEmailType.PASSWORD_RESET, requirePresent(testPersistenceQueries
                .findPasswordResetTokenByUserId(user.getId()), "Expected password reset token in concurrentFirstPasswordResetRequestsCreateOneTokenAndOneDelivery").getTokenHash());
    }

    @Test
    void accountLockRequiresAnExistingTransaction() {
        assertThatThrownBy(() -> authUserLockService.lockById(user.getId()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void concurrentRenewalsOfExistingPasswordResetPreserveOneCurrentLink() throws Exception {
        authenticationService.requestPasswordReset(user.getEmail());
        PasswordResetToken previous = requirePresent(testPersistenceQueries.findPasswordResetTokenByUserId(user.getId()), "Expected password reset token in concurrentRenewalsOfExistingPasswordResetPreserveOneCurrentLink");
        ageDeliveries(AuthEmailType.PASSWORD_RESET);

        raceWithBlockedSecond(() -> requestReset(), () -> requestReset());

        PasswordResetToken current = requirePresent(testPersistenceQueries.findPasswordResetTokenByUserId(user.getId()), "Expected password reset token in concurrentRenewalsOfExistingPasswordResetPreserveOneCurrentLink");
        assertThat(current.getId()).isEqualTo(previous.getId());
        assertThat(current.getTokenHash()).isNotEqualTo(previous.getTokenHash());
        assertOneCurrentDelivery(AuthEmailType.PASSWORD_RESET, current.getTokenHash());
        assertThat(deliveries(AuthEmailType.PASSWORD_RESET)).hasSize(2)
                .filteredOn(delivery -> delivery.getStatus() == CANCELLED).hasSize(1);
    }

    @Test
    void concurrentFirstEmailChangeRequestsCreateOneTokenAndOneDelivery() throws Exception {
        String address = "new.address@example.com";
        raceWithBlockedSecond(() -> requestEmail(address), () -> requestEmail(address));

        assertThat(emailChangeTokenRepository.findAll()).hasSize(1);
        assertOneCurrentDelivery(AuthEmailType.EMAIL_CHANGE_CONFIRMATION, requirePresent(testPersistenceQueries
                .findEmailChangeTokenByUserId(user.getId()), "Expected email change token in concurrentFirstEmailChangeRequestsCreateOneTokenAndOneDelivery").getTokenHash());
        assertThat(requirePresent(userRepository.findById(user.getId()), "Expected persisted user in concurrentFirstEmailChangeRequestsCreateOneTokenAndOneDelivery").getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    void concurrentRenewalsOfExistingEmailChangePreserveOneCurrentLink() throws Exception {
        String address = "new.address@example.com";
        emailChangeService.requestChange(user, address);
        EmailChangeToken previous = requirePresent(testPersistenceQueries.findEmailChangeTokenByUserId(user.getId()), "Expected email change token in concurrentRenewalsOfExistingEmailChangePreserveOneCurrentLink");
        ageDeliveries(AuthEmailType.EMAIL_CHANGE_CONFIRMATION);

        raceWithBlockedSecond(() -> requestEmail(address), () -> requestEmail(address));

        EmailChangeToken current = requirePresent(testPersistenceQueries.findEmailChangeTokenByUserId(user.getId()), "Expected email change token in concurrentRenewalsOfExistingEmailChangePreserveOneCurrentLink");
        assertThat(current.getId()).isEqualTo(previous.getId());
        assertThat(current.getTokenHash()).isNotEqualTo(previous.getTokenHash());
        assertOneCurrentDelivery(AuthEmailType.EMAIL_CHANGE_CONFIRMATION, current.getTokenHash());
        assertThat(deliveries(AuthEmailType.EMAIL_CHANGE_CONFIRMATION)).hasSize(2)
                .filteredOn(delivery -> delivery.getStatus() == CANCELLED).hasSize(1);
    }

    @Test
    void differentRequestedEmailsSerializeAndCancelTheSupersededDelivery() throws Exception {
        raceWithBlockedSecond(() -> requestEmail("first.change@example.com"),
                () -> requestEmail("second.change@example.com"));

        EmailChangeToken current = requirePresent(testPersistenceQueries.findEmailChangeTokenByUserId(user.getId()), "Expected email change token in differentRequestedEmailsSerializeAndCancelTheSupersededDelivery");
        assertThat(current.getPendingEmail()).isEqualTo("second.change@example.com");
        assertOneCurrentDelivery(AuthEmailType.EMAIL_CHANGE_CONFIRMATION, current.getTokenHash());
        assertThat(deliveries(AuthEmailType.EMAIL_CHANGE_CONFIRMATION)).hasSize(2)
                .filteredOn(delivery -> delivery.getStatus() == CANCELLED).hasSize(1);
    }

    @Test
    void resetConsumptionBeforeAnotherRequestAllowsANewValidTokenWithoutDeadlock() throws Exception {
        authenticationService.requestPasswordReset(user.getEmail());
        UUID oldToken = emailService.lastPasswordResetToken(user.getEmail());
        ageDeliveries(AuthEmailType.PASSWORD_RESET);

        raceWithBlockedSecond(() -> {
            authenticationService.resetPassword(oldToken, resetRequest());
            return true;
        }, () -> requestReset());

        User updated = requirePresent(userRepository.findById(user.getId()), "Expected persisted user in resetConsumptionBeforeAnotherRequestAllowsANewValidTokenWithoutDeadlock");
        assertThat(passwordEncoder.matches(UserConstants.NEW_PASSWORD, updated.getPassword())).isTrue();
        assertThat(updated.getSecurityVersion()).isEqualTo(user.getSecurityVersion() + 1);
        PasswordResetToken current = requirePresent(testPersistenceQueries.findPasswordResetTokenByUserId(user.getId()), "Expected password reset token in resetConsumptionBeforeAnotherRequestAllowsANewValidTokenWithoutDeadlock");
        assertThat(current.getTokenHash()).isNotEqualTo(AuthTokenHash.sha256(oldToken));
        assertOneCurrentDelivery(AuthEmailType.PASSWORD_RESET, current.getTokenHash());
    }

    @Test
    void renewalBeforeConsumptionRejectsTheSupersededResetToken() throws Exception {
        authenticationService.requestPasswordReset(user.getEmail());
        UUID oldToken = emailService.lastPasswordResetToken(user.getEmail());
        ageDeliveries(AuthEmailType.PASSWORD_RESET);

        RaceResult<Boolean> result = raceWithBlockedSecond(() -> requestReset(), () -> {
            authenticationService.resetPassword(oldToken, resetRequest());
            return true;
        }, PasswordResetTokenNotFoundException.class);

        assertThat(result.second()).isNull();
        assertThat(requirePresent(userRepository.findById(user.getId()), "Expected persisted user in renewalBeforeConsumptionRejectsTheSupersededResetToken").getPassword()).isEqualTo(user.getPassword());
        assertOneCurrentDelivery(AuthEmailType.PASSWORD_RESET, requirePresent(testPersistenceQueries
                .findPasswordResetTokenByUserId(user.getId()), "Expected password reset token in renewalBeforeConsumptionRejectsTheSupersededResetToken").getTokenHash());
    }

    @Test
    void emailRenewalBeforeConfirmationRejectsTheSupersededLink() throws Exception {
        emailChangeService.requestChange(user, "first.change@example.com");
        UUID oldToken = emailService.lastEmailChangeToken("first.change@example.com");

        RaceResult<EmailChangeResult> result = raceWithBlockedSecond(() -> {
            requestEmail("second.change@example.com");
            return null;
        }, () -> emailChangeService.confirmChange(oldToken));

        assertThat(result.second()).isEqualTo(EmailChangeResult.INVALID_TOKEN);
        assertThat(requirePresent(userRepository.findById(user.getId()), "Expected persisted user in emailRenewalBeforeConfirmationRejectsTheSupersededLink").getEmail()).isEqualTo(user.getEmail());
        assertOneCurrentDelivery(AuthEmailType.EMAIL_CHANGE_CONFIRMATION, requirePresent(testPersistenceQueries
                .findEmailChangeTokenByUserId(user.getId()), "Expected email change token in emailRenewalBeforeConfirmationRejectsTheSupersededLink").getTokenHash());
    }

    @Test
    void emailConfirmationBeforeResetRequestForOldAddressDoesNotIssueAReset() throws Exception {
        emailChangeService.requestChange(user, "new.address@example.com");
        UUID token = emailService.lastEmailChangeToken("new.address@example.com");

        raceWithBlockedSecond(() -> {
            assertThat(emailChangeService.confirmChange(token)).isEqualTo(EmailChangeResult.CHANGED);
            return true;
        }, () -> requestReset());

        assertThat(requirePresent(userRepository.findById(user.getId()), "Expected persisted user in emailConfirmationBeforeResetRequestForOldAddressDoesNotIssueAReset").getEmail()).isEqualTo("new.address@example.com");
        assertThat(testPersistenceQueries.findPasswordResetTokenByUserId(user.getId())).isEmpty();
        assertThat(deliveries(AuthEmailType.PASSWORD_RESET)).isEmpty();
    }

    @Test
    void resetRequestBeforeEmailConfirmationIsInvalidatedByConfirmation() throws Exception {
        emailChangeService.requestChange(user, "new.address@example.com");
        UUID token = emailService.lastEmailChangeToken("new.address@example.com");

        raceWithBlockedSecond(() -> requestReset(), () -> {
            assertThat(emailChangeService.confirmChange(token)).isEqualTo(EmailChangeResult.CHANGED);
            return true;
        });

        assertThat(testPersistenceQueries.findPasswordResetTokenByUserId(user.getId())).isEmpty();
        assertThat(testPersistenceQueries.findEmailChangeTokenByUserId(user.getId())).isEmpty();
        assertThat(deliveries(AuthEmailType.PASSWORD_RESET)).hasSize(1)
                .allMatch(delivery -> delivery.getStatus() == CANCELLED);
    }

    @Test
    void passwordValidationUsesFreshStateAfterWaitingForAReset() throws Exception {
        authenticationService.requestPasswordReset(user.getEmail());
        UUID token = emailService.lastPasswordResetToken(user.getEmail());

        RaceResult<Boolean> result = raceWithBlockedSecond(() -> {
            authenticationService.resetPassword(token, resetRequest());
            return true;
        }, () -> {
            authHelper.setupSecurityContextForFirstUser();
            userService.changeEmail(ChangeUserEmailDtoTestBuilder.validChange().build());
            return true;
        }, InvalidPasswordException.class);

        assertThat(result.second()).isNull();
        assertThat(testPersistenceQueries.findEmailChangeTokenByUserId(user.getId())).isEmpty();
    }

    @Test
    void emailConfirmationAndRefreshRotationFollowTheSameUserFirstOrder() throws Exception {
        String refreshToken = transactionTemplate.execute(ignored ->
                refreshTokenService.issueRefreshToken(requirePresent(userRepository.findById(user.getId()), "Expected persisted user in emailConfirmationAndRefreshRotationFollowTheSameUserFirstOrder"), DeviceType.WEB).rawToken());
        emailChangeService.requestChange(user, "new.address@example.com");
        UUID token = emailService.lastEmailChangeToken("new.address@example.com");

        RaceResult<Boolean> result = raceWithBlockedSecond(() -> {
            assertThat(emailChangeService.confirmChange(token)).isEqualTo(EmailChangeResult.CHANGED);
            return true;
        }, () -> {
            authenticationService.refreshAccessToken(new RefreshTokenRequestTestBuilder().refreshToken(refreshToken).build());
            return true;
        }, RefreshTokenRevokedException.class);

        assertThat(result.second()).isNull();
        assertThat(refreshTokenRepository.findAll()).hasSize(1).allMatch(tokenRow -> tokenRow.isRevoked());
    }

    @Test
    void lockingOneUserDoesNotBlockPasswordResetForAnotherUser() throws Exception {
        ExecutorService executor = TestWorkers.newSingleThreadExecutor();
        try {
            transactionTemplate.executeWithoutResult(ignored -> {
                requirePresent(authUserLockService.lockById(user.getId()), "Expected locked user in lockingOneUserDoesNotBlockPasswordResetForAnotherUser");
                Future<?> otherRequest = executor.submit(() ->
                        authenticationService.requestPasswordReset(UserConstants.SECOND_USER_EMAIL));
                try {
                    otherRequest.get(5, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            });
            User other = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.SECOND_USER_EMAIL), "Expected persisted user in lockingOneUserDoesNotBlockPasswordResetForAnotherUser");
            assertThat(testPersistenceQueries.findPasswordResetTokenByUserId(other.getId())).isPresent();
            assertThat(testPersistenceQueries.findPasswordResetTokenByUserId(user.getId())).isEmpty();
        } finally {
            TestWorkers.stop(executor);
        }
    }

    @Test
    void cleanupAndEmailConfirmationUsePasswordResetBeforeEmailChangeOrder() throws Exception {
        PasswordResetToken expired = new PasswordResetTokenTestBuilder().id(null).user(user).unissued().build();
        expired.issue(UUID.randomUUID(), 0, clock.instant().minusSeconds(1));
        passwordResetTokenRepository.saveAndFlush(expired);
        emailChangeService.requestChange(user, "new.address@example.com");
        UUID token = emailService.lastEmailChangeToken("new.address@example.com");

        RaceResult<EmailChangeResult> result = raceWithBlockedSecond(
                () -> requirePresent(passwordResetTokenRepository.findByUserIdForUpdate(user.getId()), "Expected password reset token in cleanupAndEmailConfirmationUsePasswordResetBeforeEmailChangeOrder"),
                () -> {
                    maintenanceService.cleanup();
                    return null;
                }, () -> emailChangeService.confirmChange(token));

        assertThat(result.second()).isEqualTo(EmailChangeResult.CHANGED);
        assertThat(testPersistenceQueries.findPasswordResetTokenByUserId(user.getId())).isEmpty();
        assertThat(requirePresent(userRepository.findById(user.getId()), "Expected persisted user in cleanupAndEmailConfirmationUsePasswordResetBeforeEmailChangeOrder").getEmail()).isEqualTo("new.address@example.com");
    }

    private Boolean requestReset() {
        authenticationService.requestPasswordReset(user.getEmail());
        return true;
    }

    private Boolean requestEmail(String address) {
        emailChangeService.requestChange(user, address);
        return true;
    }

    private ResetPasswordRequest resetRequest() {
        return new ResetPasswordRequestTestBuilder().password(UserConstants.NEW_PASSWORD)
                .passwordConfirmation(UserConstants.NEW_PASSWORD).build();
    }

    private List<AuthEmailDelivery> deliveries(AuthEmailType type) {
        return deliveryRepository.findAll().stream()
                .filter(delivery -> delivery.getUserId().equals(user.getId()) && delivery.getType() == type).toList();
    }

    private void ageDeliveries(AuthEmailType type) {
        deliveries(type).forEach(delivery -> {
            delivery.setCreatedAt(clock.instant().minus(Duration.ofDays(1)));
            deliveryRepository.saveAndFlush(delivery);
        });
    }

    private void assertOneCurrentDelivery(AuthEmailType type, String tokenHash) {
        List<AuthEmailDelivery> pending = deliveries(type).stream()
                .filter(delivery -> delivery.getStatus() == PENDING).toList();
        assertThat(pending).hasSize(1);
        UUID rawToken = UUID.fromString(encryptionUtils.decryptMessage(pending.getFirst().getEncryptedToken()));
        assertThat(AuthTokenHash.sha256(rawToken)).isEqualTo(tokenHash);
        if (type == AuthEmailType.EMAIL_CHANGE_CONFIRMATION) {
            assertThat(pending.getFirst().getRecipientEmail()).isEqualTo(requirePresent(testPersistenceQueries
                    .findEmailChangeTokenByUserId(user.getId()), "Expected email change token in assertOneCurrentDelivery").getPendingEmail());
        }
    }

    private <T> RaceResult<T> raceWithBlockedSecond(Supplier<T> first, Supplier<T> second) throws Exception {
        return raceWithBlockedSecond(first, second, null);
    }

    private <T> RaceResult<T> raceWithBlockedSecond(Supplier<T> first, Supplier<T> second,
                                                   Class<? extends RuntimeException> expectedFailure) throws Exception {
        return raceWithBlockedSecond(() -> requirePresent(authUserLockService.lockById(user.getId()), "Expected locked user in raceWithBlockedSecond"),
                first, second, expectedFailure);
    }

    private <T> RaceResult<T> raceWithBlockedSecond(Runnable acquireFirstLock,
                                                  Supplier<T> first, Supplier<T> second) throws Exception {
        return raceWithBlockedSecond(acquireFirstLock, first, second, null);
    }

    private <T> RaceResult<T> raceWithBlockedSecond(Runnable acquireFirstLock,
                                                  Supplier<T> first, Supplier<T> second,
                                                  Class<? extends RuntimeException> expectedFailure) throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger secondPid = new AtomicInteger();
        AtomicBoolean expectedFailureSeen = new AtomicBoolean();
        ExecutorService executor = TestWorkers.newSingleThreadExecutor();
        try {
            Future<T> competing = executor.submit(() -> {
                try {
                    return transactionTemplate.execute(ignored -> runSecond(second, ready, start, secondPid));
                } catch (RuntimeException exception) {
                    if (expectedFailure == null || !expectedFailure.isInstance(exception)) throw exception;
                    expectedFailureSeen.set(true);
                    return null;
                }
            });
            assertThat(ready.await(10, TimeUnit.SECONDS))
                    .as("Competing token request reached the start barrier").isTrue();
            T firstResult = transactionTemplate.execute(ignored -> {
                jdbcTemplate.execute("set local lock_timeout = '10s'");
                acquireFirstLock.run();
                start.countDown();
                await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(20)).untilAsserted(() ->
                        assertThat(jdbcTemplate.queryForObject(
                                "select count(*) from pg_locks where pid = ? and not granted", Integer.class,
                                secondPid.get())).isPositive());
                return first.get();
            });
            T secondResult = competing.get(20, TimeUnit.SECONDS);
            assertThat(expectedFailureSeen.get()).isEqualTo(expectedFailure != null);
            return new RaceResult<>(firstResult, secondResult);
        } finally {
            start.countDown();
            TestWorkers.stop(executor, 15);
        }
    }

    private <T> T runSecond(Supplier<T> second, CountDownLatch ready, CountDownLatch start, AtomicInteger secondPid) {
        jdbcTemplate.execute("set local lock_timeout = '10s'");
        secondPid.set(jdbcTemplate.queryForObject("select pg_backend_pid()", Integer.class));
        // Deliberately populate the persistence context before the other transaction commits.
        requirePresent(userRepository.findById(user.getId()), "Expected persisted user in runSecond");
        ready.countDown();
        try {
            if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Missing start signal");
            return second.get();
        } catch (InterruptedException exception) {
            java.lang.Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private record RaceResult<T>(T first, T second) {
    }
}
