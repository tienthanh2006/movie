package com.cinema.booking.service;

import com.cinema.booking.client.SeatClient;
import com.cinema.booking.client.ShowtimeClient;
import com.cinema.booking.dto.SeatInfo;
import com.cinema.booking.dto.ShowtimeInfo;
import com.cinema.booking.model.SeatClaim;
import com.cinema.booking.model.Ticket;
import com.cinema.booking.model.TicketStatus;
import com.cinema.booking.repository.SeatClaimRepository;
import com.cinema.booking.repository.TicketRepository;
import com.cinema.common.exception.ConflictException;
import com.cinema.common.exception.GoneException;
import com.cinema.common.exception.ResourceNotFoundException;
import com.cinema.common.messaging.BookingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);
    private final TicketRepository tickets;
    private final SeatClaimRepository claims;
    private final ShowtimeClient showtimes;
    private final SeatClient seats;
    private final StringRedisTemplate redis;
    private final BookingProducer producer;
    private final long holdSeconds;
    private final ZoneId zone;

    public TicketService(TicketRepository tickets, SeatClaimRepository claims, ShowtimeClient showtimes,
                         SeatClient seats, StringRedisTemplate redis, BookingProducer producer,
                         @Value("${cinema.seat-hold-seconds:600}") long holdSeconds,
                         @Value("${cinema.zone-id:Asia/Ho_Chi_Minh}") String timeZone) {
        if (holdSeconds < 60 || holdSeconds > 3600) throw new IllegalArgumentException("Invalid seat hold seconds");
        this.tickets = tickets;
        this.claims = claims;
        this.showtimes = showtimes;
        this.seats = seats;
        this.redis = redis;
        this.producer = producer;
        this.holdSeconds = holdSeconds;
        this.zone = ZoneId.of(timeZone);
    }

    /** Kết quả đặt vé: replayed=true nghĩa là trả lại vé đã tạo trước đó nhờ Idempotency-Key. */
    public record BookingResult(Ticket ticket, boolean replayed) {}

    private LocalDateTime now() { return LocalDateTime.now(zone); }
    private String lockKey(Long showtimeId, Long seatId) { return "seat_lock:" + showtimeId + ":" + seatId; }

    public boolean lockSeat(Long showtimeId, Long seatId, String lockToken) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(
                lockKey(showtimeId, seatId), lockToken, Duration.ofSeconds(holdSeconds)));
    }

    private void releaseLock(Long showtimeId, Long seatId, String lockToken) {
        if (lockToken == null) return;
        try {
            redis.execute(RELEASE_SCRIPT, List.of(lockKey(showtimeId, seatId)), lockToken);
        } catch (RuntimeException e) {
            log.warn("Cannot release Redis lock for showtime {} seat {}: {}", showtimeId, seatId, e.toString());
        }
    }

    private void releaseAfterCommit(Ticket ticket) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    releaseLock(ticket.getShowtimeId(), ticket.getSeatId(), ticket.getLockToken());
                }
            });
        } else {
            releaseLock(ticket.getShowtimeId(), ticket.getSeatId(), ticket.getLockToken());
        }
    }

    @Transactional
    public BookingResult createTicket(Long userId, Long showtimeId, Long seatId, String idempotencyKeyRaw) {
        if (userId == null || showtimeId == null || seatId == null || showtimeId <= 0 || seatId <= 0) {
            throw new IllegalArgumentException("userId, showtimeId and seatId are required");
        }
        // Idempotency: cùng (user, key) thì trả lại vé cũ, không tạo vé thứ hai.
        String idempotencyKey = TicketRules.normalizeIdempotencyKey(idempotencyKeyRaw);
        if (idempotencyKey != null) {
            Optional<Ticket> previous = tickets.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (previous.isPresent()) {
                Ticket old = previous.get();
                if (!old.getShowtimeId().equals(showtimeId) || !old.getSeatId().equals(seatId)) {
                    throw new ConflictException("Idempotency-Key này đã được dùng cho một yêu cầu đặt vé khác");
                }
                return new BookingResult(old, true);
            }
        }
        // Validate before opening a lock, fail closed if movie or venue service is unavailable.
        ShowtimeInfo showtime = showtimes.getShowtime(showtimeId);
        SeatInfo seat = seats.getSeat(seatId);
        if (!TicketRules.seatBelongsToRoom(seat.getRoomId(), showtime.getRoomId())) {
            throw new IllegalArgumentException("Ghế không thuộc phòng chiếu của suất này");
        }
        if (!TicketRules.seatSellable(seat.getStatus())) {
            throw new ConflictException("Ghế đang bị khóa hoặc bảo trì, vui lòng chọn ghế khác");
        }
        if ("CLOSED".equalsIgnoreCase(showtime.getStatus())) {
            throw new ConflictException("Suất chiếu đã đóng");
        }
        if (showtime.getShowDate() == null || showtime.getStartTime() == null
                || !TicketRules.isShowtimeBookable(showtime.getShowDate().atTime(showtime.getStartTime()), now())) {
            throw new ConflictException("Suất chiếu đã bắt đầu hoặc không hợp lệ");
        }
        if (showtime.getBasePrice() == null) throw new IllegalArgumentException("Suất chiếu chưa có giá");

        String token = UUID.randomUUID().toString();
        if (!lockSeat(showtimeId, seatId, token)) {
            throw new ConflictException("Ghế đang được giữ, vui lòng chọn ghế khác");
        }
        try {
            // SQL's unique claim key is the final authority after a Redis restart/TTL expiration.
            String claimKey = SeatClaim.key(showtimeId, seatId);
            SeatClaim previous = claims.findById(claimKey).orElse(null);
            if (previous != null) {
                Ticket old = tickets.findByIdForUpdate(previous.getTicketId()).orElse(null);
                if (old != null && old.getStatus() == TicketStatus.PENDING
                        && !TicketRules.holdValid(old.getExpiresAt(), now())) {
                    old.setStatus(TicketStatus.EXPIRED);
                    tickets.saveAndFlush(old);
                    claims.delete(previous);
                    claims.flush();
                } else {
                    throw new ConflictException("Ghế đã được giữ hoặc đã bán");
                }
            }
            // Legacy tickets made before the claims table existed are also considered.
            for (Ticket legacy : tickets.findByShowtimeIdAndSeatId(showtimeId, seatId)) {
                if (legacy.getStatus() == TicketStatus.PAID
                        || (legacy.getStatus() == TicketStatus.PENDING
                            && (legacy.getExpiresAt() == null || TicketRules.holdValid(legacy.getExpiresAt(), now())))) {
                    throw new ConflictException("Ghế đã được giữ hoặc đã bán");
                }
            }
            Ticket ticket = new Ticket();
            ticket.setUserId(userId);
            ticket.setShowtimeId(showtimeId);
            ticket.setSeatId(seatId);
            ticket.setFinalPrice(TicketRules.calculatePrice(showtime.getBasePrice(), seat.getSeatType()));
            ticket.setStatus(TicketStatus.PENDING);
            ticket.setCreatedAt(now());
            ticket.setExpiresAt(now().plusSeconds(holdSeconds));
            ticket.setLockToken(token);
            ticket.setIdempotencyKey(idempotencyKey);
            Ticket saved = tickets.saveAndFlush(ticket);
            claims.saveAndFlush(new SeatClaim(showtimeId, seatId, saved.getId()));
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCommit() { publishBookingEvent(saved); }
                });
            }
            return new BookingResult(saved, false);
        } catch (DataIntegrityViolationException e) {
            releaseLock(showtimeId, seatId, token);
            throw new ConflictException(idempotencyKey != null
                    ? "Yêu cầu đang được xử lý hoặc ghế vừa được đặt, vui lòng thử lại"
                    : "Ghế vừa được người khác đặt");
        } catch (RuntimeException e) {
            releaseLock(showtimeId, seatId, token);
            throw e;
        }
    }

    @Transactional
    public Ticket payTicket(Long id, Long requesterId, boolean admin) {
        Ticket ticket = lockedTicket(id);
        assertOwner(ticket, requesterId, admin);
        if (ticket.getStatus() != TicketStatus.PENDING) throw new ConflictException("Vé không ở trạng thái PENDING");
        if (!TicketRules.holdValid(ticket.getExpiresAt(), now())) {
            throw new GoneException("Vé đã hết thời gian giữ ghế");
        }
        ShowtimeInfo showtime = showtimes.getShowtime(ticket.getShowtimeId());
        if ("CLOSED".equalsIgnoreCase(showtime.getStatus())
                || showtime.getShowDate() == null || showtime.getStartTime() == null
                || !TicketRules.isShowtimeBookable(
                        showtime.getShowDate().atTime(showtime.getStartTime()), now())) {
            throw new ConflictException("Suất chiếu đã đóng hoặc bắt đầu");
        }
        String token = redis.opsForValue().get(lockKey(ticket.getShowtimeId(), ticket.getSeatId()));
        if (token == null || !token.equals(ticket.getLockToken())) {
            throw new GoneException("Khóa giữ ghế không còn hiệu lực");
        }
        SeatClaim claim = claims.findById(SeatClaim.key(ticket.getShowtimeId(), ticket.getSeatId()))
                .orElseThrow(() -> new ConflictException("Không tìm thấy quyền giữ ghế trong SQL"));
        if (!ticket.getId().equals(claim.getTicketId())) throw new ConflictException("Ghế thuộc đơn khác");
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPaidAt(now());
        Ticket saved = tickets.saveAndFlush(ticket);
        releaseAfterCommit(saved); // SeatClaim retained forever for PAID ticket.
        return saved;
    }

    @Transactional
    public Ticket cancelTicket(Long id, Long requesterId, boolean admin) {
        Ticket ticket = lockedTicket(id);
        assertOwner(ticket, requesterId, admin);
        if (ticket.getStatus() != TicketStatus.PENDING) throw new ConflictException("Chỉ có thể hủy vé PENDING");
        ticket.setStatus(TicketStatus.CANCELLED);
        releaseClaim(ticket);
        Ticket saved = tickets.saveAndFlush(ticket);
        releaseAfterCommit(saved);
        return saved;
    }

    private void expire(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.PENDING) {
            ticket.setStatus(TicketStatus.EXPIRED);
            releaseClaim(ticket);
            tickets.saveAndFlush(ticket);
            releaseAfterCommit(ticket);
        }
    }

    private void releaseClaim(Ticket ticket) {
        String key = SeatClaim.key(ticket.getShowtimeId(), ticket.getSeatId());
        claims.findById(key).ifPresent(existing -> {
            if (ticket.getId().equals(existing.getTicketId())) {
                claims.delete(existing);
                claims.flush();
            }
        });
    }

    private Ticket lockedTicket(Long id) {
        return tickets.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vé"));
    }
    private void assertOwner(Ticket ticket, Long userId, boolean admin) {
        if (!admin && !ticket.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Không có quyền với vé này");
        }
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expirePendingTickets() {
        for (Ticket pending : tickets.findByStatusAndExpiresAtBefore(TicketStatus.PENDING, now())) {
            // Lock/reload each ticket to avoid races with payment.
            tickets.findByIdForUpdate(pending.getId()).ifPresent(ticket -> {
                if (ticket.getStatus() == TicketStatus.PENDING
                        && !TicketRules.holdValid(ticket.getExpiresAt(), now())) expire(ticket);
            });
        }
    }

    private void publishBookingEvent(Ticket ticket) {
        try {
            BookingEvent event = new BookingEvent();
            event.setTicketId(ticket.getId());
            event.setUserId(ticket.getUserId());
            event.setShowtimeId(ticket.getShowtimeId());
            event.setSeatId(ticket.getSeatId());
            event.setFinalPrice(ticket.getFinalPrice());
            event.setMessage("Giữ ghế thành công, thanh toán trước thời hạn");
            producer.sendBookingEvent(event);
        } catch (Exception e) {
            log.warn("Không gửi được thông báo vé #{}: {}", ticket.getId(), e.toString());
        }
    }

    public List<Ticket> getTicketsByUserId(Long userId) { return tickets.findByUserId(userId); }
    public List<Ticket> getAllTickets() { return tickets.findAll(); }
    public Ticket getTicketById(Long id) {
        return tickets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vé"));
    }

    /** Retain admin endpoint but never allow an admin to mint paid tickets. */
    @Transactional
    public Ticket updateTicketStatus(Long id, TicketStatus newStatus) {
        if (newStatus != TicketStatus.CANCELLED) {
            throw new ConflictException("Không thể đặt PAID trực tiếp; hãy dùng luồng thanh toán");
        }
        Ticket ticket = lockedTicket(id);
        return cancelTicket(id, ticket.getUserId(), true);
    }

    @Transactional
    public void deleteTicket(Long id) {
        Ticket ticket = lockedTicket(id);
        if (ticket.getStatus() == TicketStatus.PAID || ticket.getStatus() == TicketStatus.PENDING) {
            throw new ConflictException("Không xóa vé PAID/PENDING; chỉ xóa vé đã hủy hoặc hết hạn");
        }
        tickets.delete(ticket);
    }
}
