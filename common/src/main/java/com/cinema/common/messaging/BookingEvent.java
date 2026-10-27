package com.cinema.common.messaging;

public class BookingEvent {
    private Long ticketId;
    private Long userId;
    private Long showtimeId;
    private Long seatId;
    private double finalPrice;
    private String message;

    public BookingEvent() {}

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getShowtimeId() { return showtimeId; }
    public void setShowtimeId(Long showtimeId) { this.showtimeId = showtimeId; }
    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }
    public double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(double finalPrice) { this.finalPrice = finalPrice; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
