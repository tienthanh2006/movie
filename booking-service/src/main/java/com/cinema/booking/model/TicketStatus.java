package com.cinema.booking.model;

public enum TicketStatus {
    PENDING,   // Đang giữ ghế, chờ thanh toán
    PAID,      // Đã thanh toán thành công
    CANCELLED, // Người dùng hủy
    EXPIRED    // Hết hạn giữ ghế
}
