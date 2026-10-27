package com.cinema.booking.service;

import java.time.LocalDateTime;
import java.util.Objects;

public final class TicketRules {
    private TicketRules() {}

    public static boolean isShowtimeBookable(LocalDateTime showStart, LocalDateTime now) {
        return showStart != null && showStart.isAfter(now);
    }
    public static boolean seatBelongsToRoom(Long seatRoomId, Long showtimeRoomId) {
        return seatRoomId != null && Objects.equals(seatRoomId, showtimeRoomId);
    }
    public static boolean holdValid(LocalDateTime expiresAt, LocalDateTime now) {
        return expiresAt != null && expiresAt.isAfter(now);
    }
    public static double calculatePrice(double basePrice, String seatType) {
        if (!Double.isFinite(basePrice) || basePrice <= 0) throw new IllegalArgumentException("Base price must be positive");
        return basePrice + switch (seatType == null ? "" : seatType) {
            case "VIP" -> 20000.0;
            case "SWEETBOX" -> 50000.0;
            default -> 0.0;
        };
    }

    /** Ghế chỉ bán được khi trạng thái AVAILABLE (null coi như AVAILABLE cho dữ liệu cũ). */
    public static boolean seatSellable(String seatStatus) {
        return seatStatus == null || seatStatus.isBlank() || "AVAILABLE".equalsIgnoreCase(seatStatus);
    }

    /**
     * Chuẩn hoá header Idempotency-Key. Trả null nếu không gửi; ném IllegalArgumentException nếu sai định dạng
     * (1-100 ký tự gồm chữ, số, '-', '_', '.', ':').
     */
    public static String normalizeIdempotencyKey(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String key = raw.trim();
        if (key.length() > 100 || !key.matches("[A-Za-z0-9_.:\\-]+")) {
            throw new IllegalArgumentException(
                    "Idempotency-Key chỉ gồm chữ, số, - _ . : và tối đa 100 ký tự");
        }
        return key;
    }
}
