package com.cinema.booking.controller;

import com.cinema.booking.dto.TicketRequest;
import jakarta.validation.Valid;
import com.cinema.booking.model.Ticket;
import com.cinema.booking.model.TicketStatus;
import com.cinema.booking.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Ticket API", description = "Quản lý đặt vé nâng cao")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // ===================== API CỦA KHÁCH HÀNG (cần đăng nhập) =====================
    // userId lấy từ JWT (do JwtAuthenticationFilter trong common đặt vào request)

    @Operation(summary = "1. Đặt vé mới (User lấy từ Token; header Idempotency-Key tuỳ chọn để gửi lại an toàn)")
    @PostMapping("/tickets")
    public ResponseEntity<Ticket> bookTicket(@Valid @RequestBody TicketRequest request,
                                             @RequestAttribute("userId") Long userId,
                                             @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        // BỎ QUA userId truyền từ Body. Dùng userId trong Token để tạo vé
        TicketService.BookingResult result = ticketService.createTicket(
                userId,
                request.getShowtimeId(),
                request.getSeatId(),
                idempotencyKey
        );
        if (result.replayed()) {
            // Gửi lại cùng Idempotency-Key: trả lại vé đã tạo (200), không tạo vé mới
            return ResponseEntity.ok().header("Idempotent-Replayed", "true").body(result.ticket());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.ticket());
    }

    @Operation(summary = "2. Lấy danh sách vé của TÔI (Không cần nhập ID)")
    @GetMapping("/tickets/my-tickets")
    public ResponseEntity<?> getMyTickets(@RequestAttribute("userId") Long userId) {
        List<Ticket> tickets = ticketService.getTicketsByUserId(userId);
        return ResponseEntity.ok(tickets);
    }

    @Operation(summary = "3. Lấy danh sách vé theo User ID (Có kiểm tra quyền)")
    @GetMapping("/users/{userId}/tickets")
    public ResponseEntity<?> getTicketsByUser(@PathVariable Long userId,
                                              @RequestAttribute("userId") Long currentUserId,
                                              Authentication authentication) {
        // So sánh ID trên URL với ID thực tế của Token
        if (!isAdmin(authentication) && !currentUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Lỗi 403: Bạn không có quyền xem vé của người khác!");
        }

        List<Ticket> tickets = ticketService.getTicketsByUserId(userId);
        return ResponseEntity.ok(tickets);
    }

    @Operation(summary = "4. Lấy chi tiết 1 vé (chủ vé hoặc Admin)")
    @GetMapping("/tickets/{id}")
    public ResponseEntity<?> getTicketById(@PathVariable Long id,
                                           @RequestAttribute("userId") Long currentUserId,
                                           Authentication authentication) {
        try {
            Ticket ticket = ticketService.getTicketById(id);
            if (!isAdmin(authentication) && !ticket.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Lỗi 403: Bạn không có quyền xem vé của người khác!");
            }
            return ResponseEntity.ok(ticket);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(summary = "Thanh toán giả lập cho vé đang giữ")
    @PostMapping("/tickets/{id}/pay")
    public ResponseEntity<Ticket> payTicket(@PathVariable Long id,
                                            @RequestAttribute("userId") Long userId,
                                            Authentication authentication) {
        return ResponseEntity.ok(ticketService.payTicket(id, userId, false));
    }

    @Operation(summary = "Hủy giữ ghế")
    @PostMapping("/tickets/{id}/cancel")
    public ResponseEntity<Ticket> cancelTicket(@PathVariable Long id,
                                               @RequestAttribute("userId") Long userId,
                                               Authentication authentication) {
        return ResponseEntity.ok(ticketService.cancelTicket(id, userId, isAdmin(authentication)));
    }

    // ===================== API CỦA ADMIN =====================

    @Operation(summary = "5. ADMIN: Lấy toàn bộ vé")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tickets")
    public ResponseEntity<List<Ticket>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @Operation(summary = "6. ADMIN: Cập nhật trạng thái vé (PAID/CANCELLED)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/tickets/{id}/status")
    public ResponseEntity<?> updateTicketStatus(@PathVariable Long id, @RequestParam TicketStatus status) {
        return ResponseEntity.ok(ticketService.updateTicketStatus(id, status));
    }

    @Operation(summary = "7. ADMIN: Xóa một vé")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/tickets/{id}")
    public ResponseEntity<?> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}
