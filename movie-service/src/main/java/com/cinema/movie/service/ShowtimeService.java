package com.cinema.movie.service;

import com.cinema.common.exception.ConflictException;
import com.cinema.common.exception.ResourceNotFoundException;
import com.cinema.movie.dto.ShowtimeRequest;
import com.cinema.movie.model.Movie;
import com.cinema.movie.model.Showtime;
import com.cinema.movie.repository.MovieRepository;
import com.cinema.movie.repository.RoomScheduleMutexRepository;
import com.cinema.movie.repository.ShowtimeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShowtimeService {
    private final ShowtimeRepository showtimes;
    private final MovieRepository movies;
    private final RoomScheduleMutexRepository mutexes;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public ShowtimeService(ShowtimeRepository showtimes, MovieRepository movies,
                           RoomScheduleMutexRepository mutexes, StringRedisTemplate redis, ObjectMapper mapper) {
        this.showtimes = showtimes;
        this.movies = movies;
        this.mutexes = mutexes;
        this.redis = redis;
        this.mapper = mapper;
    }

    public List<Showtime> getShowtimesByMovieAndDate(Long movieId, LocalDate showDate) {
        String key = "showtimes:movie:" + movieId + ":" + showDate;
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) return mapper.readValue(cached, new TypeReference<List<Showtime>>() {});
            List<Showtime> rows = showtimes.findByMovieIdAndShowDate(movieId, showDate);
            redis.opsForValue().set(key, mapper.writeValueAsString(rows), Duration.ofMinutes(5));
            return rows;
        } catch (Exception e) {
            return showtimes.findByMovieIdAndShowDate(movieId, showDate);
        }
    }

    public Showtime getShowtimeById(Long id) {
        return showtimes.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy suất chiếu"));
    }

    @Transactional
    public Showtime createShowtime(ShowtimeRequest request) {
        validate(request);
        Movie movie = findMovie(request.getMovieId());
        lockRooms(request.getRoomId());
        LocalDateTime end = ShowtimeRules.endingAt(request.getShowDate(), request.getStartTime(),
                movie.getDurationMinutes() == null ? 120 : movie.getDurationMinutes());
        checkRoomConflict(request.getRoomId(), request.getShowDate().atTime(request.getStartTime()), end, null);
        Showtime row = new Showtime();
        applyRequest(row, request, movie, end);
        Showtime saved = showtimes.saveAndFlush(row);
        clearShowtimeCache(movie.getId(), saved.getShowDate());
        return saved;
    }

    @Transactional
    public Showtime updateShowtime(Long id, ShowtimeRequest request) {
        validate(request);
        Showtime row = getShowtimeById(id);
        Movie movie = findMovie(request.getMovieId());
        lockRooms(row.getRoomId(), request.getRoomId());
        LocalDateTime end = ShowtimeRules.endingAt(request.getShowDate(), request.getStartTime(),
                movie.getDurationMinutes() == null ? 120 : movie.getDurationMinutes());
        checkRoomConflict(request.getRoomId(), request.getShowDate().atTime(request.getStartTime()), end, id);
        Long oldMovie = row.getMovie().getId();
        LocalDate oldDate = row.getShowDate();
        applyRequest(row, request, movie, end);
        Showtime saved = showtimes.saveAndFlush(row);
        clearShowtimeCache(oldMovie, oldDate);
        clearShowtimeCache(movie.getId(), saved.getShowDate());
        return saved;
    }

    @Transactional
    public void deleteShowtime(Long id) {
        Showtime row = getShowtimeById(id);
        lockRooms(row.getRoomId());
        showtimes.delete(row);
        showtimes.flush();
        clearShowtimeCache(row.getMovie().getId(), row.getShowDate());
    }

    private Movie findMovie(Long id) {
        return movies.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phim"));
    }
    private void validate(ShowtimeRequest req) {
        if (req.getRoomId() == null || req.getRoomId() <= 0 || req.getMovieId() == null
                || req.getShowDate() == null || req.getStartTime() == null
                || req.getBasePrice() == null || req.getBasePrice() <= 0) {
            throw new IllegalArgumentException("Thông tin suất chiếu không hợp lệ");
        }
    }

    /** Acquire DB row locks in stable order so distinct admins cannot create overlapping shows. */
    private void lockRooms(Long... roomIds) {
        java.util.Arrays.stream(roomIds).distinct().sorted().forEach(id -> {
            mutexes.ensureRoom(id);
            mutexes.lockRoom(id).orElseThrow(() -> new IllegalStateException("Room lock missing"));
        });
    }

    private void checkRoomConflict(Long roomId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        // Include adjacent day for shows crossing midnight.
        for (Showtime other : showtimes.findByRoomIdAndShowDateBetween(
                roomId, start.toLocalDate().minusDays(1), end.toLocalDate())) {
            if (excludeId != null && excludeId.equals(other.getId())) continue;
            LocalDateTime otherStart = other.getShowDate().atTime(other.getStartTime());
            if (other.getEndTime() == null) {
                throw new ConflictException("Suất chiếu cũ chưa có giờ kết thúc hợp lệ");
            }
            LocalDate otherEndDate = other.getEndDate() != null ? other.getEndDate()
                    : other.getEndTime().isBefore(other.getStartTime())
                        ? other.getShowDate().plusDays(1) : other.getShowDate();
            LocalDateTime otherEnd = otherEndDate.atTime(other.getEndTime());
            if (ShowtimeRules.overlaps(start, end, otherStart, otherEnd)) {
                throw new ConflictException("Phòng chiếu đã có suất chiếu trùng giờ");
            }
        }
    }

    private void applyRequest(Showtime row, ShowtimeRequest req, Movie movie, LocalDateTime end) {
        row.setMovie(movie);
        row.setRoomId(req.getRoomId());
        row.setShowDate(req.getShowDate());
        row.setStartTime(req.getStartTime());
        row.setEndTime(end.toLocalTime());
        row.setEndDate(end.toLocalDate());
        row.setBasePrice(req.getBasePrice());
    }

    public void clearShowtimeCache(Long movieId, LocalDate date) {
        Runnable invalidate = () -> {
            try { redis.delete("showtimes:movie:" + movieId + ":" + date); }
            catch (RuntimeException ignored) { /* TTL fallback */ }
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { invalidate.run(); }
            });
        } else {
            invalidate.run();
        }
    }
}
