package com.cinema.booking.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Thông tin ghế lấy từ venue-service (chỉ giữ phần cần dùng)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeatInfo {
    private Long id;
    private String seatName;
    private String seatType; // STANDARD, VIP, SWEETBOX
    private RoomInfo room;
    private String status; // AVAILABLE, HELD, MAINTENANCE (null = AVAILABLE)

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RoomInfo {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public RoomInfo getRoom() { return room; }
    public void setRoom(RoomInfo room) { this.room = room; }
    public Long getRoomId() { return room == null ? null : room.getId(); }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSeatName() { return seatName; }
    public void setSeatName(String seatName) { this.seatName = seatName; }
    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }
}
