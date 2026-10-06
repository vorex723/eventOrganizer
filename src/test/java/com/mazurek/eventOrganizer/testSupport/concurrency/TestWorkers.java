package com.mazurek.eventOrganizer.testSupport.concurrency;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Owns test pools only. Shared-schema tests remain sequential, not parallel-safe. */
public final class TestWorkers {
    private static final Set<ExecutorService> POOLS = ConcurrentHashMap.newKeySet();

    private TestWorkers() {
    }

    public static ExecutorService newSingleThreadExecutor() {
        return manage(Executors.newSingleThreadExecutor());
    }

    public static ExecutorService newFixedThreadPool(int threads) {
        return manage(Executors.newFixedThreadPool(threads));
    }

    static ExecutorService manage(ExecutorService delegate) {
        ExecutorService pool = new ManagedExecutor(delegate);
        POOLS.add(pool);
        return pool;
    }

    public static void stop(ExecutorService executor) {
        stop(executor, 10);
    }

    public static void stop(ExecutorService executor, long seconds) {
        try {
            executor.shutdownNow();
            if (!executor.awaitTermination(seconds, TimeUnit.SECONDS)) {
                throw new AssertionError("Test workers did not terminate within " + seconds
                        + " seconds; shared-state cleanup is forbidden");
            }
        } catch (InterruptedException exception) {
            java.lang.Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while stopping test workers; shared-state cleanup is forbidden", exception);
        } finally {
            POOLS.removeIf(ExecutorService::isTerminated);
        }
    }

    /** Also blocks the next Spring test's setup after a failed join, until workers really stop. */
    public static void requireStopped() {
        POOLS.removeIf(ExecutorService::isTerminated);
        if (!POOLS.isEmpty()) {
            throw new AssertionError("Test worker pools are still active (" + POOLS.size()
                    + "); shared-state cleanup/setup is forbidden");
        }
    }

    private static final class ManagedExecutor extends AbstractExecutorService {
        private final ExecutorService delegate;

        private ManagedExecutor(ExecutorService delegate) {
            this.delegate = delegate;
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(() -> {
                SecurityContextHolder.clearContext();
                try {
                    command.run();
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public void close() {
            stop(this);
        }
    }
}
