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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
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
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void replyCreationAdvancesVersionAndRejectsAStaleThreadEdit() throws Exception {
        var eventId = testDataInitializer.setupFirstEvent();
        var threadId = testDataInitializer.setupThreadInEventByFirstUser(eventId);
        Thread original = threadRepository.findById(threadId).orElseThrow();
        CountDownLatch snapshotLoaded = new CountDownLatch(1);
        CountDownLatch replyCommitted = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            var staleEdit = executor.submit(() -> {
                assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
                    Thread stale = threadRepository.findById(threadId).orElseThrow();
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

            Thread stored = threadRepository.findById(threadId).orElseThrow();
            assertThat(stored.getVersion()).isEqualTo(original.getVersion() + 1);
            assertThat(stored.getReplyCount()).isEqualTo(1);
            assertThat(stored.getContent()).isEqualTo(original.getContent());
            assertThat(threadReplyRepository.count()).isEqualTo(1);
        } finally {
            replyCommitted.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void bulkReplyUpdateAdvancesVersionWithoutMovingLastActivityBackwards() {
        var eventId = testDataInitializer.setupFirstEvent();
        var threadId = testDataInitializer.setupThreadInEventByFirstUser(eventId);
        Thread original = threadRepository.findById(threadId).orElseThrow();

        transactionTemplate.executeWithoutResult(status -> assertThat(
                threadRepository.incrementReplyCountAndAdvanceLastActivity(threadId, original.getLastActivity().minusSeconds(1)))
                .isEqualTo(1));

        Thread stored = threadRepository.findById(threadId).orElseThrow();
        assertThat(stored.getVersion()).isEqualTo(original.getVersion() + 1);
        assertThat(stored.getReplyCount()).isEqualTo(original.getReplyCount() + 1);
        assertThat(stored.getLastActivity()).isEqualTo(original.getLastActivity());
    }

    @Test
    void profileUpdateRefreshesItsStaleSnapshotAndPreservesSecurityAndPreferenceChanges() throws Exception {
        User original = userRepository.findByIgnoreCaseEmail(UserConstants.FIRST_USER_EMAIL).orElseThrow();
        CountDownLatch snapshotLoaded = new CountDownLatch(1);
        CountDownLatch securityCommitted = new CountDownLatch(1);
        String changedPassword = passwordEncoder.encode(UserConstants.NEW_PASSWORD);
        var credentialsChangedAt = TimeConstants.NOW.plusSeconds(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
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
                User locked = authUserLockService.lockById(original.getId()).orElseThrow();
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

            User stored = userRepository.findById(original.getId()).orElseThrow();
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
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
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
