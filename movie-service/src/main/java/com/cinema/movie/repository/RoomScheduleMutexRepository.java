package com.cinema.movie.repository;

import com.cinema.movie.model.RoomScheduleMutex;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface RoomScheduleMutexRepository extends JpaRepository<RoomScheduleMutex, Long> {
    // MySQL INSERT IGNORE handles concurrent initial writers atomically.
    @Modifying
    @Query(value = "INSERT IGNORE INTO room_schedule_mutex(room_id) VALUES (:roomId)", nativeQuery = true)
    void ensureRoom(@Param("roomId") Long roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from RoomScheduleMutex m where m.roomId = :roomId")
    Optional<RoomScheduleMutex> lockRoom(@Param("roomId") Long roomId);
}
