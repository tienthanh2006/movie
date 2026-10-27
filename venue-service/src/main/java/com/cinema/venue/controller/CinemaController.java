package com.cinema.venue.controller;

import com.cinema.venue.model.Cinema;
import com.cinema.venue.service.CinemaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CinemaController {

    private final CinemaService cinemaService;

    public CinemaController(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    // ĐỌC: Lấy danh sách rạp (Ai cũng xem được, có Cache Redis 60 phút)
    @GetMapping("/v1/cinemas")
    public ResponseEntity<List<Cinema>> getAllCinemas() {
        return ResponseEntity.ok(cinemaService.getAllCinemas());
    }

    // GHI: Thêm rạp mới (Chỉ Admin mới có quyền)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/v1/cinemas")
    public ResponseEntity<Cinema> createCinema(@RequestBody Cinema cinema) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cinemaService.saveCinema(cinema));
    }

    // XÓA: Xóa rạp theo ID (Chỉ Admin mới có quyền)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/v1/cinemas/{id}")
    public ResponseEntity<Void> deleteCinema(@PathVariable Long id) {
        cinemaService.deleteCinema(id);
        return ResponseEntity.noContent().build();
    }
}
