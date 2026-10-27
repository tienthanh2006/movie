package com.cinema.movie.service;

import com.cinema.movie.model.Movie;
import com.cinema.movie.repository.MovieRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public MovieService(MovieRepository movieRepository, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.movieRepository = movieRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // --- CÁC HÀM XỬ LÝ CACHE DANH SÁCH PHIM (TTL 10 PHÚT) ---

    public List<Movie> getNowShowingMovies() {
        return getMoviesWithCache("movies:now_showing", "Đang chiếu");
    }

    public List<Movie> getComingSoonMovies() {
        return getMoviesWithCache("movies:coming_soon", "Sắp chiếu");
    }

    private List<Movie> getMoviesWithCache(String cacheKey, String status) {
        try {
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                return objectMapper.readValue(cachedData, new TypeReference<List<Movie>>(){});
            }

            List<Movie> movies = movieRepository.findByStatus(status);
            String moviesJson = objectMapper.writeValueAsString(movies);
            redisTemplate.opsForValue().set(cacheKey, moviesJson, Duration.ofMinutes(10));
            return movies;
        } catch (Exception e) {
            return movieRepository.findByStatus(status);
        }
    }

    // --- CÁC HÀM GHI DỮ LIỆU (PHẢI XÓA CACHE LIÊN QUAN) ---

    private void clearMovieListCaches() {
        redisTemplate.delete(Arrays.asList("movies:now_showing", "movies:coming_soon"));
    }

    public Movie createMovie(Movie movie) {
        Movie savedMovie = movieRepository.save(movie);
        clearMovieListCaches();
        return savedMovie;
    }

    public Movie updateMovie(Long id, Movie movieDetails) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phim!"));

        movie.setTitle(movieDetails.getTitle());
        movie.setStatus(movieDetails.getStatus());
        movie.setGenre(movieDetails.getGenre());
        movie.setReleaseYear(movieDetails.getReleaseYear());
        movie.setDurationMinutes(movieDetails.getDurationMinutes());

        Movie updatedMovie = movieRepository.save(movie);

        redisTemplate.delete("movie:" + id);
        clearMovieListCaches();

        return updatedMovie;
    }

    public void deleteMovie(Long id) {
        movieRepository.deleteById(id);
        redisTemplate.delete("movie:" + id);
        clearMovieListCaches();
    }

    // --- CÁC HÀM CŨ GIỮ NGUYÊN ---

    public Movie getMovieById(Long id) {
        String cacheKey = "movie:" + id;
        try {
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                return objectMapper.readValue(cachedData, Movie.class);
            }
            Movie movie = movieRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy phim!"));
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(movie), Duration.ofMinutes(30));
            return movie;
        } catch (Exception e) {
            return movieRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy phim!"));
        }
    }

    public Page<Movie> getMovies(String title, String genre, Pageable pageable) {
        if (title != null && genre != null) {
            return movieRepository.findByTitleContainingIgnoreCaseAndGenreContainingIgnoreCase(title, genre, pageable);
        } else if (title != null) {
            return movieRepository.findByTitleContainingIgnoreCase(title, pageable);
        } else if (genre != null) {
            return movieRepository.findByGenreContainingIgnoreCase(genre, pageable);
        }
        return movieRepository.findAll(pageable);
    }
}
