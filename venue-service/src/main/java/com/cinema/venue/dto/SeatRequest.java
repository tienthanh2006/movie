package com.cinema.venue.dto;

import com.cinema.venue.model.SeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SeatRequest {

    // Bắt buộc khi thêm ghế mới, bỏ qua khi sửa ghế
    private Long roomId;

    @NotBlank(message = "Tên ghế không được để trống")
    private String seatName;

    @NotNull(message = "Loại ghế không được để trống (STANDARD, VIP, SWEETBOX)")
    private SeatType seatType;

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getSeatName() { return seatName; }
    public void setSeatName(String seatName) { this.seatName = seatName; }
    public SeatType getSeatType() { return seatType; }
    public void setSeatType(SeatType seatType) { this.seatType = seatType; }
}
