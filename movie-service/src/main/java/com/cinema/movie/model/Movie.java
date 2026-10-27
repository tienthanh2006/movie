package com.cinema.movie.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên phim không được để trống")
    private String title;

    @NotBlank(message = "Thể loại không được để trống")
    private String genre;

    @Min(value = 1888, message = "Năm phát hành không hợp lệ")
    private int releaseYear;

    @Min(value = 1, message = "Thời lượng phim phải > 0")
    private Integer durationMinutes = 120;

    @NotBlank(message = "Trạng thái không được để trống")
    private String status;

    public Movie() {}
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public Movie(String title, String genre, int releaseYear, String status) {
        this.title = title;
        this.genre = genre;
        this.releaseYear = releaseYear;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public int getReleaseYear() { return releaseYear; }
    public void setReleaseYear(int releaseYear) { this.releaseYear = releaseYear; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
