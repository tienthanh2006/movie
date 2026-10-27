package com.cinema.booking.repository;

import com.cinema.booking.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;
import com.cinema.booking.model.TicketStatus;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByUserId(Long userId); // Lấy lịch sử mua vé của user
    List<Ticket> findByShowtimeId(Long showtimeId);
    List<Ticket> findByStatusAndExpiresAtBefore(TicketStatus status, LocalDateTime now);
    List<Ticket> findByShowtimeIdAndSeatId(Long showtimeId, Long seatId);
    Optional<Ticket> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> findByIdForUpdate(@Param("id") Long id);
}
