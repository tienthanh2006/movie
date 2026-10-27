package com.cinema.venue.dto;

import com.cinema.venue.model.SeatStatus;
import com.cinema.venue.model.SeatType;

/** Body của PATCH /api/v1/seats/{id}: chỉ gửi trường nào cần đổi, trường không gửi (null) giữ nguyên. */
public class SeatPatchRequest {
    private SeatStatus status;
    private SeatType seatType;

    public SeatStatus getStatus() { return status; }
    public void setStatus(SeatStatus status) { this.status = status; }
    public SeatType getSeatType() { return seatType; }
    public void setSeatType(SeatType seatType) { this.seatType = seatType; }
}
