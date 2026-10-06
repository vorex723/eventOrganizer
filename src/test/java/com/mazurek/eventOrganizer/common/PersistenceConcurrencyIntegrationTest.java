package com.mazurek.eventOrganizer.common;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.AuthUserLockService;
import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestDataInitializer;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserDetailsDtoTestBuilder;
import com.mazurek.eventOrganizer.thread.Thread;
import com.mazurek.eventOrganizer.thread.ThreadRepository;
import com.mazurek.eventOrganizer.threadReply.ThreadReplyRepository;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import com.mazurek.eventOrganizer.testSupport.concurrency.TestWorkers;
import java.util.concurrent.TimeUnit;

import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("PersistenceConcurrencyIntegrationTest contracts:")
class PersistenceConcurrencyIntegrationTest {
    @Autowired private DeletionService deletionService;
    @Autowired private AuthHelper authHelper;
    @Autowired private TestDataInitializer testDataInitializer;
    @Autowired private ThreadRepository threadRepository;
    @Autowired private ThreadReplyRepository threadReplyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;
    @Autowired private AuthenticationService authenticationService;
    @Autowired private AuthUserLockService authUserLockService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TransactionTemplate transactionTemplate;
    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Test
    void whenReplyIsCreatedConcurrentlyShouldAdvanceVersionAndRejectStaleEdit() throws Exception {
        var eventId = testDataInitializer.setupFirstEvent();
        var threadId = testDataInitializer.setupThreadInEventByFirstUser(eventId);
        Thread original = requirePresent(threadRepository.findById(threadId), "Expected baseline/model prerequisite in whenReplyIsCreatedConcurrentlyShouldAdvanceVersionAndRejectStaleEdit");
        CountDownLatch snapshotLoaded = new CountDownLatch(1);
        CountDownLatch replyCommitted = new CountDownLatch(1);
        ExecutorService executor = TestWorkers.newSingleThreadExecutor();
        try {
            var staleEdit = executor.submit(() -> {
                assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
                    Thread stale = requirePresent(threadRepository.findById(threadId), "Expected baseline/model prerequisite in whenReplyIsCreatedConcurrentlyShouldAdvanceVersionAndRejectStaleEdit");
                    snapshotLoaded.countDown();
                    await(replyCommitted);
                    stale.setContent("Stale edit must not overwrite the new reply count");
                    threadRepository.saveAndFlush(stale);
                })).isInstanceOf(OptimisticLockingFailureException.class);
            });
            await(snapshotLoaded);
            testDataInitializer.setupThreadReplyInThreadByFirstUser(eventId, threadId);
            replyCommitted.countDown();
            staleEdit.get(10, TimeUnit.SECONDS);

            Thread stored = requirePresent(threadRepository.findById(threadId), "Expected baseline/model prerequisite in whenReplyIsCreatedConcurrentlyShouldAdvanceVersionAndRejectStaleEdit");
            assertThat(stored.getVersion()).isEqualTo(original.getVersion() + 1);
            assertThat(stored.getReplyCount()).isEqualTo(1);
            assertThat(stored.getContent()).isEqualTo(original.getContent());
            assertThat(threadReplyRepository.count()).isEqualTo(1);
        } finally {
            replyCommitted.countDown();
            TestWorkers.stop(executor);
        }
    }

    @Test
    void whenReplyCountIsUpdatedShouldAdvanceVersionWithoutRegressingActivity() {
        var eventId = testDataInitializer.setupFirstEvent();
        var threadId = testDataInitializer.setupThreadInEventByFirstUser(eventId);
        Thread original = requirePresent(threadRepository.findById(threadId), "Expected baseline/model prerequisite in whenReplyCountIsUpdatedShouldAdvanceVersionWithoutRegressingActivity");

        transactionTemplate.executeWithoutResult(status -> assertThat(
                threadRepository.incrementReplyCountAndAdvanceLastActivity(threadId, original.getLastActivity().minusSeconds(1)))
                .isEqualTo(1));

        Thread stored = requirePresent(threadRepository.findById(threadId), "Expected baseline/model prerequisite in whenReplyCountIsUpdatedShouldAdvanceVersionWithoutRegressingActivity");
        assertThat(stored.getVersion()).isEqualTo(original.getVersion() + 1);
        assertThat(stored.getReplyCount()).isEqualTo(original.getReplyCount() + 1);
        assertThat(stored.getLastActivity()).isEqualTo(original.getLastActivity());
    }

    @Test
    void whenProfileSnapshotIsStaleShouldPreserveConcurrentSecurityAndPreferenceChanges() throws Exception {
        User original = requirePresent(userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL), "Expected baseline/model prerequisite in whenProfileSnapshotIsStaleShouldPreserveConcurrentSecurityAndPreferenceChanges");
        CountDownLatch snapshotLoaded = new CountDownLatch(1);
        CountDownLatch securityCommitted = new CountDownLatch(1);
        String changedPassword = passwordEncoder.encode(UserConstants.NEW_PASSWORD);
        var credentialsChangedAt = TimeConstants.NOW.plusSeconds(1);
        ExecutorService executor = TestWorkers.newSingleThreadExecutor();
        try {
            var profileUpdate = executor.submit(() -> {
                try {
                    authHelper.setupSecurityContextForFirstUser();
                    transactionTemplate.executeWithoutResult(status -> {
                        User stale = authenticationService.getCurrentUser();
                        var details = ChangeUserDetailsDtoTestBuilder.validUpdate()
                                .homeCityExternalId(stale.getHomeCity().getExternalId())
                                .build();
                        snapshotLoaded.countDown();
                        await(securityCommitted);
                        userService.changeDetails(details);
                    });
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
            await(snapshotLoaded);
            transactionTemplate.executeWithoutResult(status -> {
                User locked = requirePresent(authUserLockService.lockById(original.getId()), "Expected baseline/model prerequisite in whenProfileSnapshotIsStaleShouldPreserveConcurrentSecurityAndPreferenceChanges");
                locked.setPassword(changedPassword);
                locked.setLastCredentialsChangeTime(credentialsChangedAt);
                locked.setSecurityVersion(original.getSecurityVersion() + 1);
                locked.setBanned(true);
                locked.setActivated(false);
                userRepository.saveAndFlush(locked);
                assertThat(userRepository.advanceNotificationPreferencesVersion(
                        original.getId(), original.getNotificationPreferencesVersion())).isEqualTo(1);
            });
            securityCommitted.countDown();
            profileUpdate.get(10, TimeUnit.SECONDS);

            User stored = requirePresent(userRepository.findById(original.getId()), "Expected baseline/model prerequisite in whenProfileSnapshotIsStaleShouldPreserveConcurrentSecurityAndPreferenceChanges");
            assertThat(stored.getFirstName()).isEqualTo(UserConstants.SECOND_USER_FIRST_NAME);
            assertThat(stored.getLastName()).isEqualTo(UserConstants.SECOND_USER_LAST_NAME);
            assertThat(stored.getTimeZone()).isEqualTo(UserConstants.SECOND_USER_TIMEZONE);
            assertThat(stored.getPassword()).isEqualTo(changedPassword);
            assertThat(stored.getLastCredentialsChangeTime()).isEqualTo(credentialsChangedAt);
            assertThat(stored.getSecurityVersion()).isEqualTo(original.getSecurityVersion() + 1);
            assertThat(stored.getNotificationPreferencesVersion()).isEqualTo(original.getNotificationPreferencesVersion() + 1);
            assertThat(stored.isBanned()).isTrue();
            assertThat(stored.isActivated()).isFalse();
        } finally {
            securityCommitted.countDown();
            TestWorkers.stop(executor);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).as("Concurrent transaction reached its checkpoint").isTrue();
        } catch (InterruptedException exception) {
            java.lang.Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent test interrupted", exception);
        }
    }
}
