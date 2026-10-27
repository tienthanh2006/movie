package com.cinema.venue.repository;

import com.cinema.venue.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    // Tìm toàn bộ ghế thuộc về một phòng (Room)
    List<Seat> findByRoomId(Long roomId);

    // Kiểm tra tên ghế đã tồn tại trong phòng chưa
    boolean existsByRoomIdAndSeatName(Long roomId, String seatName);
}
