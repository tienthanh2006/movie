package com.cinema.booking.model;

import jakarta.persistence.*;

@Entity
@Table(name = "seat_claims", uniqueConstraints = @UniqueConstraint(
        name = "uk_seat_claim_showtime_seat", columnNames = {"showtime_id", "seat_id"}))
public class SeatClaim {
    @Id
    @Column(name = "claim_key", nullable = false, length = 100)
    private String claimKey;
    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;
    @Column(name = "seat_id", nullable = false)
    private Long seatId;
    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;
    protected SeatClaim() {}
    public SeatClaim(Long showtimeId, Long seatId, Long ticketId) {
        this.claimKey = key(showtimeId, seatId);
        this.showtimeId = showtimeId;
        this.seatId = seatId;
        this.ticketId = ticketId;
    }
    public static String key(Long showtimeId, Long seatId) { return showtimeId + ":" + seatId; }
    public String getClaimKey() { return claimKey; }
    public Long getTicketId() { return ticketId; }
}
