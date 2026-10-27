package com.cinema.common.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryExecutorTest {
    private static boolean isIo(RuntimeException e) { return e instanceof IllegalStateException; }

    @Test void returnsImmediatelyOnSuccess() {
        AtomicInteger calls = new AtomicInteger();
        String r = RetryExecutor.execute(() -> { calls.incrementAndGet(); return "ok"; },
                RetryExecutorTest::isIo, 3, 0, e -> "fallback", ms -> {});
        assertEquals("ok", r);
        assertEquals(1, calls.get());
    }

    @Test void retriesThenSucceeds() {
        AtomicInteger calls = new AtomicInteger();
        List<Long> waits = new ArrayList<>();
        String r = RetryExecutor.execute(() -> {
            if (calls.incrementAndGet() < 3) throw new IllegalStateException("down");
            return "ok";
        }, RetryExecutorTest::isIo, 3, 100, e -> "fallback", waits::add);
        assertEquals("ok", r);
        assertEquals(3, calls.get());
        assertEquals(List.of(100L, 200L), waits);
    }

    @Test void usesFallbackAfterExhaustingAttempts() {
        AtomicInteger calls = new AtomicInteger();
        String r = RetryExecutor.execute(() -> { calls.incrementAndGet(); throw new IllegalStateException("down"); },
                RetryExecutorTest::isIo, 3, 0, e -> "fallback:" + e.getMessage(), ms -> {});
        assertEquals("fallback:down", r);
        assertEquals(3, calls.get());
    }

    @Test void doesNotRetryNonRetryableErrors() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(IllegalArgumentException.class, () -> RetryExecutor.execute(
                () -> { calls.incrementAndGet(); throw new IllegalArgumentException("404"); },
                RetryExecutorTest::isIo, 3, 0, e -> "fallback", ms -> {}));
        assertEquals(1, calls.get());
    }
}
