package com.cinema.venue.service;

import com.cinema.venue.model.Cinema;
import com.cinema.venue.repository.CinemaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class CinemaService {

    private final CinemaRepository cinemaRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CinemaService(CinemaRepository cinemaRepository, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.cinemaRepository = cinemaRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // ĐỌC: Lấy danh sách rạp có Cache Redis (TTL 60 phút)
    public List<Cinema> getAllCinemas() {
        String cacheKey = "cinemas:all";
        try {
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                return objectMapper.readValue(cachedData, new TypeReference<List<Cinema>>(){});
            }

            List<Cinema> cinemas = cinemaRepository.findAll();
            String jsonVal = objectMapper.writeValueAsString(cinemas);
            redisTemplate.opsForValue().set(cacheKey, jsonVal, Duration.ofMinutes(60));
            return cinemas;
        } catch (Exception e) {
            return cinemaRepository.findAll();
        }
    }

    // GHI: Thêm/Sửa/Xóa rạp phải xóa cache liên quan
    public Cinema saveCinema(Cinema cinema) {
        Cinema saved = cinemaRepository.save(cinema);
        redisTemplate.delete("cinemas:all");
        return saved;
    }

    public void deleteCinema(Long id) {
        cinemaRepository.deleteById(id);
        redisTemplate.delete("cinemas:all");
    }
}
