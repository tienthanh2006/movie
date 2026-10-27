package com.cinema.movie.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

public class ShowtimeRequest {

    @NotNull(message = "movieId không được để trống")
    private Long movieId;

    @NotNull(message = "roomId không được để trống")
    private Long roomId;

    @NotNull(message = "Ngày chiếu không được để trống")
    @FutureOrPresent(message = "Ngày chiếu không được ở quá khứ")
    private LocalDate showDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    // Deprecated input retained for API compatibility; server computes end time.
    private LocalTime endTime;

    @NotNull(message = "Giá vé gốc không được để trống")
    @Positive(message = "Giá vé gốc phải lớn hơn 0")
    private Double basePrice;

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDate getShowDate() { return showDate; }
    public void setShowDate(LocalDate showDate) { this.showDate = showDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Double getBasePrice() { return basePrice; }
    public void setBasePrice(Double basePrice) { this.basePrice = basePrice; }
}
