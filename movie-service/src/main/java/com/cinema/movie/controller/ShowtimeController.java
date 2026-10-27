package com.cinema.movie.controller;

import com.cinema.movie.dto.ShowtimeRequest;
import com.cinema.movie.model.Showtime;
import com.cinema.movie.service.ShowtimeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/showtimes")
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    public ShowtimeController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    // API Lấy lịch chiếu theo ngày (có Redis Cache 5 phút)
    @GetMapping
    public ResponseEntity<List<Showtime>> getShowtimesByMovieAndDate(
            @RequestParam Long movieId,
            @RequestParam String date) {

        LocalDate showDate = LocalDate.parse(date);
        List<Showtime> showtimes = showtimeService.getShowtimesByMovieAndDate(movieId, showDate);
        return ResponseEntity.ok(showtimes);
    }

    // Lấy 1 suất chiếu theo id (booking-service dùng để lấy giá gốc)
    @GetMapping("/{id}")
    public ResponseEntity<Showtime> getShowtimeById(@PathVariable Long id) {
        return ResponseEntity.ok(showtimeService.getShowtimeById(id));
    }

    // ===================== ADMIN =====================

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Showtime> createShowtime(@Valid @RequestBody ShowtimeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(showtimeService.createShowtime(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Showtime> updateShowtime(@PathVariable Long id,
                                                   @Valid @RequestBody ShowtimeRequest request) {
        return ResponseEntity.ok(showtimeService.updateShowtime(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShowtime(@PathVariable Long id) {
        showtimeService.deleteShowtime(id);
        return ResponseEntity.noContent().build();
    }
}
