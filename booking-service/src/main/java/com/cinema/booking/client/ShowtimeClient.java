package com.cinema.booking.client;

import com.cinema.booking.dto.ShowtimeInfo;
import com.cinema.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

// Gọi sang movie-service (có timeout + retry + fallback 503)
@Component
public class ShowtimeClient {

    private final RestClient restClient;
    private final RemoteCaller remote;

    public ShowtimeClient(@Value("${services.movie.url}") String baseUrl, RemoteCaller remote) {
        this.remote = remote;
        this.restClient = remote.newClient(baseUrl);
    }

    public ShowtimeInfo getShowtime(Long showtimeId) {
        return remote.get("movie-service", () -> {
            try {
                return restClient.get()
                        .uri("/api/v1/showtimes/{id}", showtimeId)
                        .retrieve()
                        .body(ShowtimeInfo.class);
            } catch (HttpClientErrorException.NotFound e) {
                throw new ResourceNotFoundException("Không tìm thấy Suất chiếu");
            } catch (HttpClientErrorException e) {
                throw new IllegalStateException("movie-service trả lỗi " + e.getStatusCode().value());
            }
        });
    }
}
