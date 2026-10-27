package com.cinema.venue.controller;

import com.cinema.venue.dto.SeatPatchRequest;
import com.cinema.venue.dto.SeatRequest;
import com.cinema.venue.model.Seat;
import com.cinema.venue.service.SeatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seats")
@Tag(name = "Seat API", description = "Quản lý sơ đồ ghế ngồi")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @Operation(summary = "Lấy sơ đồ ghế theo ID Phòng chiếu")
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<Seat>> getSeatsByRoomId(@PathVariable Long roomId) {
        List<Seat> seats = seatService.getSeatsByRoomId(roomId);
        return ResponseEntity.ok(seats);
    }

    @Operation(summary = "Lấy thông tin 1 ghế theo ID (booking-service dùng)")
    @GetMapping("/{id}")
    public ResponseEntity<Seat> getSeatById(@PathVariable Long id) {
        return ResponseEntity.ok(seatService.getSeatById(id));
    }

    // ===================== ADMIN =====================

    @Operation(summary = "ADMIN: Thêm 1 ghế vào phòng")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Seat> createSeat(@Valid @RequestBody SeatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seatService.createSeat(request));
    }

    @Operation(summary = "ADMIN: Sửa tên hoặc loại ghế")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Seat> updateSeat(@PathVariable Long id, @Valid @RequestBody SeatRequest request) {
        return ResponseEntity.ok(seatService.updateSeat(id, request));
    }

    @Operation(summary = "ADMIN: Cập nhật một phần ghế (giữ/nhả ghế: status; đổi loại ghế: seatType)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<Seat> patchSeat(@PathVariable Long id, @RequestBody SeatPatchRequest request) {
        return ResponseEntity.ok(seatService.patchSeat(id, request));
    }

    @Operation(summary = "ADMIN: Xóa 1 ghế")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeat(@PathVariable Long id) {
        seatService.deleteSeat(id);
        return ResponseEntity.noContent().build();
    }
}
