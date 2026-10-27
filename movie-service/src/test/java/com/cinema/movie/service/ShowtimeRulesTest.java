package com.cinema.movie.service;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class ShowtimeRulesTest {
    @Test void derivesEndTimeAndCleanup() {
        var date=LocalDate.of(2026,10,25);
        assertEquals(date.atTime(21,15), ShowtimeRules.endingAt(date, LocalTime.of(19,0),120));
        assertEquals(date.plusDays(1), ShowtimeRules.endingAt(date,LocalTime.of(23,50),120).toLocalDate());
    }
    @Test void detectsOverlapButNotTouching() {
        var s=LocalDate.of(2026,10,25).atTime(19,0);
        var e=s.plusHours(2);
        assertTrue(ShowtimeRules.overlaps(s,e,s.plusMinutes(60),e.plusHours(1)));
        assertFalse(ShowtimeRules.overlaps(s,e,e,e.plusHours(1)));
        assertFalse(ShowtimeRules.overlaps(s,e,s.minusHours(1),s));
    }
}
