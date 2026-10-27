package com.cinema.venue.model;

public enum SeatStatus {
    AVAILABLE,   // Ghế bình thường, bán được
    HELD,        // Admin giữ ghế thủ công (không bán)
    MAINTENANCE  // Ghế hỏng / đang bảo trì (không bán)
}
