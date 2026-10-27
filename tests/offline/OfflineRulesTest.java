import com.cinema.booking.service.TicketRules;
import com.cinema.movie.service.ShowtimeRules;
import java.time.*;

public class OfflineRulesTest {
    private static int assertions;
    private static void yes(boolean condition, String name) {
        assertions++;
        if (!condition) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.of(2026, 10, 25, 19, 0);
        yes(TicketRules.isShowtimeBookable(now.plusMinutes(1), now), "future show");
        yes(!TicketRules.isShowtimeBookable(now, now), "current show");
        yes(!TicketRules.isShowtimeBookable(now.minusNanos(1), now), "past show");
        yes(!TicketRules.isShowtimeBookable(null, now), "unknown show");
        yes(TicketRules.seatBelongsToRoom(5L, 5L), "same room");
        yes(!TicketRules.seatBelongsToRoom(5L, 6L), "other room");
        yes(!TicketRules.seatBelongsToRoom(null, 5L), "missing room");
        yes(TicketRules.holdValid(now.plusSeconds(1), now), "valid hold");
        yes(!TicketRules.holdValid(now, now), "expired exactly now");
        yes(!TicketRules.holdValid(null, now), "missing expiry");
        yes(TicketRules.calculatePrice(80000, "STANDARD") == 80000, "standard price");
        yes(TicketRules.calculatePrice(80000, "VIP") == 100000, "vip price");
        yes(TicketRules.calculatePrice(80000, "SWEETBOX") == 130000, "sweetbox price");
        try { TicketRules.calculatePrice(0, "VIP"); throw new AssertionError("invalid price allowed"); }
        catch (IllegalArgumentException ok) { assertions++; }
        LocalDate date = LocalDate.of(2026,10,25);
        LocalDateTime start = date.atTime(19, 0);
        LocalDateTime end = ShowtimeRules.endingAt(date, LocalTime.of(19,0), 120);
        yes(end.equals(date.atTime(21,15)), "cleanup included");
        yes(ShowtimeRules.overlaps(start,end,start.plusHours(1),end.plusHours(1)), "overlap");
        yes(!ShowtimeRules.overlaps(start,end,end,end.plusMinutes(30)), "touching intervals");
        yes(!ShowtimeRules.overlaps(start,end,start.minusHours(2),start), "ends exactly start");
        yes(ShowtimeRules.endingAt(date,LocalTime.of(23,50),120).toLocalDate().equals(date.plusDays(1)), "overnight");
        try { ShowtimeRules.endingAt(date,LocalTime.NOON,0); throw new AssertionError("invalid duration allowed"); }
        catch (IllegalArgumentException ok) { assertions++; }
        System.out.println("PASS " + assertions + " standalone JVM business-rule assertions");
    }
}
