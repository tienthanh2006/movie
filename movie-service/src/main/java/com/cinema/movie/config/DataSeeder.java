package com.cinema.movie.config;

import com.cinema.movie.model.Movie;
import com.cinema.movie.model.Showtime;
import com.cinema.movie.repository.MovieRepository;
import com.cinema.movie.repository.ShowtimeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.LocalDate;
import java.time.LocalTime;

// Thay cho TestController cũ: tự tạo dữ liệu mẫu lần chạy đầu tiên
@Component
@ConditionalOnProperty(name = "cinema.demo-data.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;

    public DataSeeder(MovieRepository movieRepository, ShowtimeRepository showtimeRepository) {
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Override
    public void run(String... args) {
        if (movieRepository.count() > 0) {
            return;
        }

        Movie movie = new Movie("Phim Test Hệ Thống Mới", "Hành động", 2026, "Đang chiếu");
        movieRepository.save(movie);

        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoomId(1L); // Phòng 1 do venue-service tạo sẵn
        showtime.setBasePrice(80000.0);
        showtime.setShowDate(LocalDate.now().plusDays(1));
        showtime.setStartTime(LocalTime.of(19, 0));
        showtime.setEndTime(LocalTime.of(21, 0));
        showtimeRepository.save(showtime);

        System.out.println("====== MOVIE-SERVICE: ĐÃ TẠO DỮ LIỆU MẪU (suất chiếu id " + showtime.getId() + ") ======");
    }
}
