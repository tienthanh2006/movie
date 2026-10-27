package com.cinema.venue.controller;

import com.cinema.venue.dto.GenerateSeatsRequest;
import com.cinema.venue.dto.RoomRequest;
import com.cinema.venue.model.Room;
import com.cinema.venue.model.Seat;
import com.cinema.venue.repository.SeatRepository;
import com.cinema.venue.service.RoomService;
import com.cinema.venue.service.SeatService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RoomController {

    private final SeatRepository seatRepository;
    private final RoomService roomService;
    private final SeatService seatService;

    public RoomController(SeatRepository seatRepository, RoomService roomService, SeatService seatService) {
        this.seatRepository = seatRepository;
        this.roomService = roomService;
        this.seatService = seatService;
    }

    @Operation(summary = "Lấy sơ đồ ghế của một phòng chiếu")
    @GetMapping("/v1/rooms/{id}/seats")
    public ResponseEntity<List<Seat>> getSeatsByRoomId(@PathVariable Long id) {
        List<Seat> seats = seatRepository.findByRoomId(id);
        return ResponseEntity.ok(seats);
    }

    // ===================== ADMIN =====================

    @Operation(summary = "ADMIN: Thêm phòng chiếu")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/v1/rooms")
    public ResponseEntity<Room> createRoom(@Valid @RequestBody RoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(request));
    }

    @Operation(summary = "ADMIN: Sửa phòng chiếu")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/v1/rooms/{id}")
    public ResponseEntity<Room> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(id, request));
    }

    @Operation(summary = "ADMIN: Xóa phòng chiếu (xóa luôn ghế của phòng)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/v1/rooms/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "ADMIN: Tạo hàng loạt sơ đồ ghế cho phòng")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/v1/rooms/{roomId}/seats/generate")
    public ResponseEntity<List<Seat>> generateSeats(@PathVariable Long roomId,
                                                    @Valid @RequestBody GenerateSeatsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seatService.generateSeats(roomId, request));
    }
}
