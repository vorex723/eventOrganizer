package com.mazurek.eventOrganizer.testSupport.email;

import com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryService;
import com.mazurek.eventOrganizer.notification.service.RecordingEmailService;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.lang.reflect.Method;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_ID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

/** Exercises real Spring TestContext callbacks on an owned, database-free context. */
@DisplayName("RecordingEmailState integration tests:")
class RecordingEmailStateIntegrationTest {

    private final UUID previousToken = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private final UUID currentToken = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");

    @Test
    void whenSpringDiscoversDefaultListenersShouldRegisterRecordingCleanupExactlyOnce() {
        TestContextManager manager = new TestContextManager(FirstProbe.class);

        assertThat(manager.getTestExecutionListeners())
                .filteredOn(RecordingEmailStateTestExecutionListener.class::isInstance)
                .hasSize(1);
        assertThat(manager.getTestContext().hasApplicationContext()).isFalse();
    }

    @ParameterizedTest(name = "Scenario outcome: {0}")
    @ValueSource(strings = {"success", "assertion failure", "setup failure"})
    void whenScenarioFinishesShouldClearRecordingsDespiteItsFailure(String outcome) throws Exception {
        TestContextManager manager = new TestContextManager(FirstProbe.class);
        FirstProbe probe = new FirstProbe();
        Method method = FirstProbe.class.getDeclaredMethod("scenario");
        try {
            manager.beforeTestClass();
            manager.prepareTestInstance(probe);
            RecordingEmailService service = manager.getTestContext().getApplicationContext()
                    .getBean(RecordingEmailService.class);
            recordAllTokens(service, previousToken);

            manager.beforeTestMethod(probe, method);
            assertRecordedTokens(service, null);
            // Models recordings created by user setup/Act, before an exception or success.
            recordAllTokens(service, currentToken);
            assertRecordedTokens(service, currentToken);
            Throwable failure = switch (outcome) {
                case "assertion failure" -> new AssertionError("Deliberate assertion failure");
                case "setup failure" -> new IllegalStateException("Deliberate setup failure");
                default -> null;
            };

            manager.afterTestMethod(probe, method, failure);

            assertRecordedTokens(service, null);
        } finally {
            closeOwnedContext(manager);
        }
    }

    @Test
    void whenAnotherCleanupListenerFailsShouldStillResetRecordingsAndPropagateFailure() throws Exception {
        TestContextManager manager = new TestContextManager(FirstProbe.class);
        FirstProbe probe = new FirstProbe();
        Method method = FirstProbe.class.getDeclaredMethod("scenario");
        IllegalStateException failure = new IllegalStateException("Simulated database cleanup failure");
        // Register last so its after callback fails BEFORE recording cleanup (reverse order).
        manager.registerTestExecutionListeners(new AbstractTestExecutionListener() {
            @Override
            public void afterTestMethod(TestContext testContext) {
                throw failure;
            }
        });
        try {
            manager.beforeTestClass();
            manager.prepareTestInstance(probe);
            manager.beforeTestMethod(probe, method);
            RecordingEmailService service = manager.getTestContext().getApplicationContext()
                    .getBean(RecordingEmailService.class);
            recordAllTokens(service, previousToken);

            assertThatThrownBy(() -> manager.afterTestMethod(probe, method, null)).isSameAs(failure);

            assertRecordedTokens(service, null);
        } finally {
            closeOwnedContext(manager);
        }
    }

    @Test
    void whenTwoClassesReuseCachedContextShouldStartWithFreshRecordingsAndKeepTheSameService() throws Exception {
        TestContextManager first = new TestContextManager(FirstProbe.class);
        TestContextManager second = new TestContextManager(SecondProbe.class);
        FirstProbe firstProbe = new FirstProbe();
        SecondProbe secondProbe = new SecondProbe();
        Method firstMethod = FirstProbe.class.getDeclaredMethod("scenario");
        Method secondMethod = SecondProbe.class.getDeclaredMethod("scenario");
        boolean firstClassFinished = false;
        try {
            first.beforeTestClass();
            first.prepareTestInstance(firstProbe);
            first.beforeTestMethod(firstProbe, firstMethod);
            RecordingEmailService service = first.getTestContext().getApplicationContext()
                    .getBean(RecordingEmailService.class);
            recordAllTokens(service, previousToken);
            first.afterTestMethod(firstProbe, firstMethod, null);
            first.afterTestClass();
            firstClassFinished = true;

            second.beforeTestClass();
            second.prepareTestInstance(secondProbe);
            assertThat(second.getTestContext().getApplicationContext()
                    .getBean(RecordingEmailService.class)).isSameAs(service);
            // A stale out-of-method recording must also be cleared by BEFORE, not only AFTER.
            recordAllTokens(service, previousToken);
            second.beforeTestMethod(secondProbe, secondMethod);
            assertRecordedTokens(service, null);
            recordAllTokens(service, currentToken);
            assertRecordedTokens(service, currentToken);
            second.afterTestMethod(secondProbe, secondMethod, null);
            assertRecordedTokens(service, null);
        } finally {
            // Drop only this owned focused context, not another suite's cached context.
            try {
                second.afterTestClass();
            } finally {
                try {
                    if (!firstClassFinished) {
                        first.afterTestClass();
                    }
                } finally {
                    discardOwnedContext(first.getTestContext());
                }
            }
        }
    }

    private void closeOwnedContext(TestContextManager manager) throws Exception {
        TestContext testContext = manager.getTestContext();
        try {
            manager.afterTestClass();
        } finally {
            discardOwnedContext(testContext);
        }
    }

    private void discardOwnedContext(TestContext testContext) {
        if (testContext.hasApplicationContext()) {
            testContext.markApplicationContextDirty(HierarchyMode.CURRENT_LEVEL);
        }
    }

    private void recordAllTokens(RecordingEmailService service, UUID token) {
        service.sendActivationEmail(FIRST_USER_EMAIL, token);
        service.sendPasswordResetEmail(FIRST_USER_EMAIL, token);
        service.sendEmailChangeConfirmationEmail(FIRST_USER_ID, FIRST_USER_EMAIL, token);
    }

    private void assertRecordedTokens(RecordingEmailService service, UUID token) {
        assertThat(service.lastActivationToken(FIRST_USER_EMAIL)).isEqualTo(token);
        assertThat(service.lastPasswordResetToken(FIRST_USER_EMAIL)).isEqualTo(token);
        assertThat(service.lastEmailChangeToken(FIRST_USER_EMAIL)).isEqualTo(token);
    }

    // These classes contain no JUnit tests; they are metadata for explicit callback probes.
    @ContextConfiguration(classes = RecordingConfiguration.class)
    static class FirstProbe {
        void scenario() {}
    }

    @ContextConfiguration(classes = RecordingConfiguration.class)
    static class SecondProbe {
        void scenario() {}
    }

    @Configuration(proxyBeanMethods = false)
    static class RecordingConfiguration {
        @Bean
        RecordingEmailService recordingEmailService() {
            return new RecordingEmailService(mock(UserRepository.class), mock(AuthEmailDeliveryService.class));
        }
    }
}
