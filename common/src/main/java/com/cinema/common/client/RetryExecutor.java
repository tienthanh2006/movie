package com.cinema.common.client;

import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Retry + Fallback thuần Java (không phụ thuộc Spring) cho các lời gọi service-to-service.
 *
 * - Chỉ retry khi lỗi thoả {@code retryable} (mất kết nối, timeout, HTTP 5xx).
 * - Lỗi không retry được (vd 404) ném ra ngay, không tốn thêm lần thử.
 * - Hết số lần thử thì gọi {@code fallback}; fallback có thể ném ServiceUnavailableException (fail-closed).
 * - Chỉ nên dùng cho thao tác an toàn khi lặp lại (GET).
 */
public final class RetryExecutor {
    private RetryExecutor() {}

    public static <T> T execute(Supplier<T> call,
                                Predicate<RuntimeException> retryable,
                                int maxAttempts,
                                long backoffMillis,
                                Function<RuntimeException, T> fallback) {
        return execute(call, retryable, maxAttempts, backoffMillis, fallback, RetryExecutor::sleep);
    }

    /** Bản có tham số sleeper để kiểm thử không phải chờ thật. */
    public static <T> T execute(Supplier<T> call,
                                Predicate<RuntimeException> retryable,
                                int maxAttempts,
                                long backoffMillis,
                                Function<RuntimeException, T> fallback,
                                LongConsumer sleeper) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.get();
            } catch (RuntimeException e) {
                if (!retryable.test(e)) throw e;
                last = e;
                if (attempt < maxAttempts) sleeper.accept(backoffMillis * attempt);
            }
        }
        return fallback.apply(last);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
