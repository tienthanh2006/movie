package com.cinema.venue.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "seats", uniqueConstraints = @UniqueConstraint(name = "uk_room_seat_name", columnNames = {"room_id", "seat_name"}))
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seat_name", nullable = false)
    private String seatName; // Ví dụ: A1, A2, G10

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatType seatType;

    // Trạng thái ghế ở cấp phòng chiếu (cập nhật một phần bằng PATCH). Dữ liệu cũ chưa có cột -> coi là AVAILABLE
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", length = 20)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    public Seat() {}

    public SeatStatus getStatus() { return status == null ? SeatStatus.AVAILABLE : status; }
    public void setStatus(SeatStatus status) { this.status = status; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSeatName() { return seatName; }
    public void setSeatName(String seatName) { this.seatName = seatName; }
    public SeatType getSeatType() { return seatType; }
    public void setSeatType(SeatType seatType) { this.seatType = seatType; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
}
