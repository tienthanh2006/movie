package com.cinema.booking.service;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class TicketRulesTest {
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 25, 12, 0);

    @Test void rejectsPastAndCurrentShows() {
        assertFalse(TicketRules.isShowtimeBookable(now.minusSeconds(1), now));
        assertFalse(TicketRules.isShowtimeBookable(now, now));
        assertTrue(TicketRules.isShowtimeBookable(now.plusSeconds(1), now));
    }
    @Test void checksSeatRoomAndHoldExpiry() {
        assertTrue(TicketRules.seatBelongsToRoom(10L, 10L));
        assertFalse(TicketRules.seatBelongsToRoom(10L, 20L));
        assertFalse(TicketRules.seatBelongsToRoom(null, 20L));
        assertFalse(TicketRules.holdValid(now, now));
        assertTrue(TicketRules.holdValid(now.plusSeconds(1), now));
    }
    @Test void handlesSurcharges() {
        assertEquals(80_000, TicketRules.calculatePrice(80_000, "STANDARD"));
        assertEquals(100_000, TicketRules.calculatePrice(80_000, "VIP"));
        assertEquals(130_000, TicketRules.calculatePrice(80_000, "SWEETBOX"));
        assertThrows(IllegalArgumentException.class, () -> TicketRules.calculatePrice(-1, "VIP"));
    }
    @Test void seatStatusSellableOnlyWhenAvailable() {
        assertTrue(TicketRules.seatSellable(null));
        assertTrue(TicketRules.seatSellable("AVAILABLE"));
        assertFalse(TicketRules.seatSellable("HELD"));
        assertFalse(TicketRules.seatSellable("MAINTENANCE"));
    }
    @Test void validatesIdempotencyKey() {
        assertNull(TicketRules.normalizeIdempotencyKey(null));
        assertNull(TicketRules.normalizeIdempotencyKey("   "));
        assertEquals("order-123:a.b_c", TicketRules.normalizeIdempotencyKey("  order-123:a.b_c "));
        assertThrows(IllegalArgumentException.class, () -> TicketRules.normalizeIdempotencyKey("có dấu cách"));
        assertThrows(IllegalArgumentException.class, () -> TicketRules.normalizeIdempotencyKey("x".repeat(101)));
    }
}
