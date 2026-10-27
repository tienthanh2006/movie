package com.cinema.movie.repository;

import com.cinema.movie.model.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    // Lấy tất cả lịch chiếu của một phim
    List<Showtime> findByMovieId(Long movieId);

    // Lấy lịch chiếu theo phim VÀ ngày chiếu
    List<Showtime> findByMovieIdAndShowDate(Long movieId, LocalDate showDate);

    // Lấy các suất của một phòng trong một ngày (để kiểm tra trùng giờ)
    List<Showtime> findByRoomIdAndShowDate(Long roomId, LocalDate showDate);
    List<Showtime> findByRoomIdAndShowDateBetween(Long roomId, LocalDate start, LocalDate end);
}
