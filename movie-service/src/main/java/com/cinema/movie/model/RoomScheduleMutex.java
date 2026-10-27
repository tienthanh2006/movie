package com.cinema.movie.model;

import jakarta.persistence.*;

@Entity
@Table(name = "room_schedule_mutex")
public class RoomScheduleMutex {
    @Id
    @Column(name = "room_id")
    private Long roomId;
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
}
