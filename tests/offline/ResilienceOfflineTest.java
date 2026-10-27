import com.cinema.booking.service.TicketRules;
import com.cinema.common.client.RetryExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Kiểm thử thuần Java (không cần Maven/Spring) cho retry/fallback và quy tắc Idempotency-Key, trạng thái ghế. */
public class ResilienceOfflineTest {
    private static int n;
    private static void yes(boolean c, String name) { n++; if (!c) throw new AssertionError(name); }
    private static boolean isIo(RuntimeException e) { return e instanceof IllegalStateException; }

    public static void main(String[] a) {
        AtomicInteger calls = new AtomicInteger();
        yes("ok".equals(RetryExecutor.execute(() -> { calls.incrementAndGet(); return "ok"; },
                ResilienceOfflineTest::isIo, 3, 0, e -> "fb", ms -> {})) && calls.get() == 1, "success first try");

        calls.set(0);
        List<Long> waits = new ArrayList<>();
        String r = RetryExecutor.execute(() -> { if (calls.incrementAndGet() < 3) throw new IllegalStateException("x"); return "ok"; },
                ResilienceOfflineTest::isIo, 3, 100, e -> "fb", waits::add);
        yes("ok".equals(r) && calls.get() == 3 && waits.equals(List.of(100L, 200L)), "retry then success with backoff");

        calls.set(0);
        r = RetryExecutor.execute(() -> { calls.incrementAndGet(); throw new IllegalStateException("down"); },
                ResilienceOfflineTest::isIo, 3, 0, e -> "fb:" + e.getMessage(), ms -> {});
        yes("fb:down".equals(r) && calls.get() == 3, "fallback after 3 attempts");

        calls.set(0);
        boolean thrown = false;
        try { RetryExecutor.execute(() -> { calls.incrementAndGet(); throw new IllegalArgumentException("404"); },
                ResilienceOfflineTest::isIo, 3, 0, e -> "fb", ms -> {}); }
        catch (IllegalArgumentException e) { thrown = true; }
        yes(thrown && calls.get() == 1, "non-retryable not retried");

        boolean bad = false;
        try { RetryExecutor.execute(() -> "x", ResilienceOfflineTest::isIo, 0, 0, e -> "fb", ms -> {}); }
        catch (IllegalArgumentException e) { bad = true; }
        yes(bad, "maxAttempts < 1 rejected");

        yes(TicketRules.seatSellable(null) && TicketRules.seatSellable("AVAILABLE"), "seat sellable");
        yes(!TicketRules.seatSellable("HELD") && !TicketRules.seatSellable("MAINTENANCE"), "seat not sellable");
        yes(TicketRules.normalizeIdempotencyKey(null) == null, "no key");
        yes(TicketRules.normalizeIdempotencyKey("  abc-1:2_3.4 ").equals("abc-1:2_3.4"), "key trimmed");
        boolean badKey = false;
        try { TicketRules.normalizeIdempotencyKey("co dau cach"); } catch (IllegalArgumentException e) { badKey = true; }
        yes(badKey, "space in key rejected");
        badKey = false;
        try { TicketRules.normalizeIdempotencyKey("x".repeat(101)); } catch (IllegalArgumentException e) { badKey = true; }
        yes(badKey, "long key rejected");

        System.out.println("PASS " + n + " kiểm tra (retry/fallback, idempotency key, trạng thái ghế)");
    }
}
