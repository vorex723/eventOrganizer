package com.mazurek.eventOrganizer.testSupport.concurrency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("Test workers ownership unit tests:")
class TestWorkersUnitTest {
    @BeforeEach
    void setUp() {
        TestWorkers.requireStopped();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TestWorkers.requireStopped();
    }

    @Test
    void whenTaskCompletesShouldClearPrincipalBeforeReusingThreadAndJoinBeforeCleanup() throws Exception {
        try (ExecutorService executor = TestWorkers.newSingleThreadExecutor()) {
            assertThatThrownBy(TestWorkers::requireStopped).isInstanceOf(AssertionError.class);
            executor.submit(() -> SecurityContextHolder.getContext()
                    .setAuthentication(new TestingAuthenticationToken("worker", null))).get(5, TimeUnit.SECONDS);
            assertThat(executor.submit(() -> SecurityContextHolder.getContext().getAuthentication())
                    .get(5, TimeUnit.SECONDS)).isNull();
        }
        TestWorkers.requireStopped();
    }

    @Test
    void whenTaskFailsShouldClearPrincipalAndRetainFailure() throws Exception {
        RuntimeException failure = new RuntimeException("task failed");
        try (ExecutorService executor = TestWorkers.newSingleThreadExecutor()) {
            var result = executor.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("worker", null));
                throw failure;
            });
            assertThatThrownBy(() -> result.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class).hasCause(failure);
            assertThat(executor.submit(() -> SecurityContextHolder.getContext().getAuthentication())
                    .get(5, TimeUnit.SECONDS)).isNull();
        }
    }

    @Test
    void whenTerminationTimesOutShouldKeepCleanupBlockedUntilActualTermination() throws Exception {
        ExecutorService delegate = mock(ExecutorService.class);
        ExecutorService pool = TestWorkers.manage(delegate);
        try {
            assertThatThrownBy(() -> TestWorkers.stop(pool)).isInstanceOf(AssertionError.class)
                    .hasMessageContaining("within 10 seconds");
            verify(delegate).shutdownNow();
            verify(delegate).awaitTermination(10, TimeUnit.SECONDS);
            assertThatThrownBy(TestWorkers::requireStopped).isInstanceOf(AssertionError.class);
        } finally {
            when(delegate.isTerminated()).thenReturn(true);
        }
        TestWorkers.requireStopped();
    }

    @Test
    void whenJoinIsInterruptedShouldRestoreInterruptAndBlockCleanup() throws Exception {
        ExecutorService delegate = mock(ExecutorService.class);
        InterruptedException failure = new InterruptedException("join interrupted");
        when(delegate.awaitTermination(10, TimeUnit.SECONDS)).thenThrow(failure);
        ExecutorService pool = TestWorkers.manage(delegate);
        try {
            assertThatThrownBy(() -> TestWorkers.stop(pool)).isInstanceOf(AssertionError.class).hasCause(failure);
            assertThat(java.lang.Thread.currentThread().isInterrupted()).isTrue();
            assertThatThrownBy(TestWorkers::requireStopped).isInstanceOf(AssertionError.class);
        } finally {
            java.lang.Thread.interrupted();
            when(delegate.isTerminated()).thenReturn(true);
        }
    }

    @Test
    void whenCloseFailsShouldPreserveOriginalFailureWithTerminationFailureSuppressed() {
        ExecutorService delegate = mock(ExecutorService.class);
        RuntimeException failure = new RuntimeException("original assertion failed");
        try {
            assertThatThrownBy(() -> {
                try (ExecutorService pool = TestWorkers.manage(delegate)) {
                    throw failure;
                }
            }).isSameAs(failure);
            assertThat(failure.getSuppressed()).hasSize(1).allSatisfy(suppressed ->
                    assertThat(suppressed).isInstanceOf(AssertionError.class).hasMessageContaining("cleanup is forbidden"));
        } finally {
            when(delegate.isTerminated()).thenReturn(true);
        }
    }
}
