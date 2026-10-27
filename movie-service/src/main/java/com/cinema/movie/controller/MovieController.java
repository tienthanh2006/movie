package com.cinema.movie.controller;

import com.cinema.movie.model.Movie;
import com.cinema.movie.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Movie API", description = "Quản lý thông tin phim")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @Operation(summary = "Lấy danh sách phim Đang chiếu (Cache 10p)")
    @GetMapping("/v1/movies/now-showing")
    public ResponseEntity<List<Movie>> getNowShowingMovies() {
        return ResponseEntity.ok(movieService.getNowShowingMovies());
    }

    @Operation(summary = "Lấy danh sách phim Sắp chiếu (Cache 10p)")
    @GetMapping("/v1/movies/coming-soon")
    public ResponseEntity<List<Movie>> getComingSoonMovies() {
        return ResponseEntity.ok(movieService.getComingSoonMovies());
    }

    @Operation(summary = "Lấy danh sách phim (Hỗ trợ tìm kiếm, phân trang)")
    @GetMapping("/v1/movies")
    public ResponseEntity<Page<Movie>> getAllMoviesV1(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            Pageable pageable) {
        return ResponseEntity.ok(movieService.getMovies(title, genre, pageable));
    }

    @Operation(summary = "Lấy thông tin phim theo ID (Cache 30p)")
    @GetMapping("/v1/movies/{id}")
    public ResponseEntity<Movie> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    @Operation(summary = "Thêm mới một bộ phim")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/v1/movies")
    public ResponseEntity<Movie> createMovie(@Valid @RequestBody Movie movie) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movieService.createMovie(movie));
    }

    @Operation(summary = "Cập nhật thông tin phim")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/v1/movies/{id}")
    public ResponseEntity<Movie> updateMovie(@PathVariable Long id, @Valid @RequestBody Movie movieDetails) {
        return ResponseEntity.ok(movieService.updateMovie(id, movieDetails));
    }

    @Operation(summary = "Xóa phim theo ID")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/v1/movies/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}
