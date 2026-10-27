package com.cinema.movie.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class ShowtimeRules {
    private ShowtimeRules() {}

    public static LocalDateTime endingAt(LocalDate date, LocalTime start, int durationMinutes) {
        if (date == null || start == null || durationMinutes <= 0 || durationMinutes > 600) {
            throw new IllegalArgumentException("Invalid showtime or movie duration");
        }
        return date.atTime(start).plusMinutes(durationMinutes).plusMinutes(15);
    }
    public static boolean overlaps(LocalDateTime start, LocalDateTime end,
                                   LocalDateTime otherStart, LocalDateTime otherEnd) {
        if (start == null || end == null || otherStart == null || otherEnd == null
                || !end.isAfter(start) || !otherEnd.isAfter(otherStart)) {
            throw new IllegalArgumentException("Invalid showtime interval");
        }
        return start.isBefore(otherEnd) && otherStart.isBefore(end);
    }
}
