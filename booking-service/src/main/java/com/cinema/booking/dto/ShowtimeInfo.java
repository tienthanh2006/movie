package com.cinema.booking.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Thông tin suất chiếu lấy từ movie-service (chỉ giữ phần cần dùng)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShowtimeInfo {
    private Long id;
    private Double basePrice;
    private Long roomId;
    private java.time.LocalDate showDate;
    private java.time.LocalTime startTime;
    private String status;

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public java.time.LocalDate getShowDate() { return showDate; }
    public void setShowDate(java.time.LocalDate showDate) { this.showDate = showDate; }
    public java.time.LocalTime getStartTime() { return startTime; }
    public void setStartTime(java.time.LocalTime startTime) { this.startTime = startTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Double getBasePrice() { return basePrice; }
    public void setBasePrice(Double basePrice) { this.basePrice = basePrice; }
}
