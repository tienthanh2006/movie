package com.cinema.movie.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "showtimes")
public class Showtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "movie_id")
    private Movie movie;

    // Phòng chiếu nằm ở venue-service nên chỉ lưu id
    private Long roomId;

    private LocalDate showDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate endDate;
    private String status = "OPEN";

    private Double basePrice;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public LocalDate getShowDate() { return showDate; }
    public void setShowDate(LocalDate showDate) { this.showDate = showDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getStatus() { return status == null ? "OPEN" : status; }
    public void setStatus(String status) { this.status = status; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Double getBasePrice() {
        return basePrice != null ? basePrice : 0.0;
    }
    public void setBasePrice(Double basePrice) {
        this.basePrice = basePrice;
    }
}
