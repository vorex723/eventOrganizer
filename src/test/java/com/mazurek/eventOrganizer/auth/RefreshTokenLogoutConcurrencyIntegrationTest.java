package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.auth.dto.AuthenticationResponse;
import com.mazurek.eventOrganizer.auth.dto.RefreshTokenRequest;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenRevokedException;
import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.jwt.RefreshToken;
import com.mazurek.eventOrganizer.jwt.RefreshTokenRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.TestPersistenceQueries;
import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.RefreshTokenTestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenLogoutConcurrencyIntegrationTest {

    @Autowired
    private TestPersistenceQueries testPersistenceQueries;

    @Autowired private DeletionService deletionService;
    @Autowired private AuthHelper authHelper;
    @Autowired private AuthenticationService authenticationService;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private TransactionTemplate transactionTemplate;

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
    void refreshBeforeLogoutLeavesRotatedSuccessorActive() {
        String original = issueWebToken();

        String successor = authenticationService.refreshAccessToken(new RefreshTokenRequest(original))
                .getRefreshToken();
        authenticationService.logout(new RefreshTokenRequest(original));

        assertThat(successor).isNotEqualTo(original);
        assertThat(findToken(original).isRevoked()).isTrue();
        assertThat(findToken(successor).isRevoked()).isFalse();
        assertThat(findToken(successor).getFamilyId()).isEqualTo(findToken(original).getFamilyId());
        assertThat(familyTokens(findToken(original).getFamilyId())).hasSize(2);
    }

    @Test
    void logoutBeforeRefreshRejectsRefreshWithoutCreatingSuccessor() {
        String original = issueWebToken();
        UUID familyId = findToken(original).getFamilyId();
        authenticationService.logout(new RefreshTokenRequest(original));

        assertThatThrownBy(() -> authenticationService.refreshAccessToken(new RefreshTokenRequest(original)))
                .isInstanceOf(RefreshTokenRevokedException.class);

        assertThat(findToken(original).isRevoked()).isTrue();
        assertThat(familyTokens(familyId)).hasSize(1);
    }

    @Test
    void concurrentLogoutAndRefreshSerializeOnTheSameToken() throws Exception {
        String original = issueWebToken();
        String unrelated = issueWebToken();
        UUID familyId = findToken(original).getFamilyId();
        assertThat(findToken(unrelated).getFamilyId()).isNotEqualTo(familyId);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Optional<String>> refresh = executor.submit(() -> {
                awaitStart(ready, start);
                try {
                    AuthenticationResponse response = authenticationService.refreshAccessToken(
                            new RefreshTokenRequest(original));
                    return Optional.of(response.getRefreshToken());
                } catch (RefreshTokenRevokedException exception) {
                    return Optional.empty();
                }
            });
            Future<Void> logout = executor.submit(() -> {
                awaitStart(ready, start);
                authenticationService.logout(new RefreshTokenRequest(original));
                return null;
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

            transactionTemplate.executeWithoutResult(ignored -> {
                assertThat(refreshTokenRepository.findWithLockByTokenHash(
                        RefreshTokenTestBuilder.hashOf(original))).isPresent();
                start.countDown();
                assertThatThrownBy(() -> refresh.get(200, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);
                assertThatThrownBy(() -> logout.get(200, TimeUnit.MILLISECONDS))
                        .isInstanceOf(TimeoutException.class);
            });

            Optional<String> successor = refresh.get(10, TimeUnit.SECONDS);
            logout.get(10, TimeUnit.SECONDS);

            assertThat(findToken(original).isRevoked()).isTrue();
            assertThat(findToken(unrelated).isRevoked()).isFalse();
            List<RefreshToken> family = familyTokens(familyId);
            if (successor.isPresent()) {
                assertThat(successor.orElseThrow()).isNotEqualTo(original);
                assertThat(family).hasSize(2);
                assertThat(findToken(successor.orElseThrow()).isRevoked()).isFalse();
                assertThat(findToken(successor.orElseThrow()).getFamilyId()).isEqualTo(familyId);
            } else {
                assertThat(family).hasSize(1);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private String issueWebToken() {
        return authenticationService.authenticate(
                AuthenticationRequestTestBuilder.authenticationRequestForFirstUser().build(),
                DeviceType.WEB).getRefreshToken();
    }

    private RefreshToken findToken(String rawToken) {
        return testPersistenceQueries.findRefreshTokenByHash(RefreshTokenTestBuilder.hashOf(rawToken)).orElseThrow();
    }

    private List<RefreshToken> familyTokens(UUID familyId) {
        return refreshTokenRepository.findAll().stream()
                .filter(token -> token.getFamilyId().equals(familyId))
                .toList();
    }

    private void awaitStart(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent test did not receive its start signal.");
        }
    }
}
