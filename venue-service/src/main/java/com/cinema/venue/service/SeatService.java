package com.cinema.venue.service;

import com.cinema.common.exception.ConflictException;
import com.cinema.common.exception.ResourceNotFoundException;
import com.cinema.venue.dto.GenerateSeatsRequest;
import com.cinema.venue.dto.SeatPatchRequest;
import com.cinema.venue.dto.SeatRequest;
import com.cinema.venue.model.Room;
import com.cinema.venue.model.Seat;
import com.cinema.venue.model.SeatType;
import com.cinema.venue.repository.RoomRepository;
import com.cinema.venue.repository.SeatRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final RoomRepository roomRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SeatService(SeatRepository seatRepository, RoomRepository roomRepository,
                       StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.seatRepository = seatRepository;
        this.roomRepository = roomRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // ĐỌC: Lấy danh sách toàn bộ ghế của một phòng chiếu có Cache Redis (TTL 60 phút)
    public List<Seat> getSeatsByRoomId(Long roomId) {
        String cacheKey = "screen:" + roomId + ":seats";
        try {
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                return objectMapper.readValue(cachedData, new TypeReference<List<Seat>>(){});
            }

            List<Seat> seats = seatRepository.findByRoomId(roomId);

            String jsonVal = objectMapper.writeValueAsString(seats);
            redisTemplate.opsForValue().set(cacheKey, jsonVal, Duration.ofMinutes(60));

            return seats;
        } catch (Exception e) {
            return seatRepository.findByRoomId(roomId);
        }
    }

    // booking-service gọi API này để lấy loại ghế (tính phụ thu)
    public Seat getSeatById(Long id) {
        return seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Ghế"));
    }

    // ===================== ADMIN: THÊM / SỬA / XÓA GHẾ =====================

    public Seat createSeat(SeatRequest request) {
        if (request.getRoomId() == null) {
            throw new RuntimeException("roomId không được để trống");
        }
        Room room = findRoom(request.getRoomId());

        if (seatRepository.existsByRoomIdAndSeatName(room.getId(), request.getSeatName())) {
            throw new ConflictException("Ghế " + request.getSeatName() + " đã tồn tại trong phòng này");
        }

        Seat seat = new Seat();
        seat.setSeatName(request.getSeatName());
        seat.setSeatType(request.getSeatType());
        seat.setRoom(room);
        Seat saved = seatRepository.save(seat);

        clearScreenSeatsCache(room.getId());
        return saved;
    }

    public Seat updateSeat(Long id, SeatRequest request) {
        Seat seat = getSeatById(id);
        Long roomId = seat.getRoom().getId();

        boolean nameChanged = !seat.getSeatName().equals(request.getSeatName());
        if (nameChanged && seatRepository.existsByRoomIdAndSeatName(roomId, request.getSeatName())) {
            throw new ConflictException("Ghế " + request.getSeatName() + " đã tồn tại trong phòng này");
        }

        seat.setSeatName(request.getSeatName());
        seat.setSeatType(request.getSeatType());
        Seat saved = seatRepository.save(seat);

        clearScreenSeatsCache(roomId);
        return saved;
    }

    // PATCH: cập nhật MỘT PHẦN ghế (trạng thái và/hoặc loại ghế), trường nào null thì giữ nguyên
    public Seat patchSeat(Long id, SeatPatchRequest request) {
        if (request.getStatus() == null && request.getSeatType() == null) {
            throw new IllegalArgumentException("Cần gửi ít nhất một trường để cập nhật: status hoặc seatType");
        }
        Seat seat = getSeatById(id);
        if (request.getStatus() != null) {
            seat.setStatus(request.getStatus());
        }
        if (request.getSeatType() != null) {
            seat.setSeatType(request.getSeatType());
        }
        Seat saved = seatRepository.save(seat);
        clearScreenSeatsCache(seat.getRoom().getId());
        return saved;
    }

    public void deleteSeat(Long id) {
        Seat seat = getSeatById(id);
        Long roomId = seat.getRoom().getId();
        seatRepository.delete(seat);
        clearScreenSeatsCache(roomId);
    }

    // Tạo hàng loạt ghế cho một phòng (bỏ qua ghế đã tồn tại)
    public List<Seat> generateSeats(Long roomId, GenerateSeatsRequest request) {
        Room room = findRoom(roomId);
        Set<String> vipRows = toUpperSet(request.getVipRows());
        Set<String> sweetboxRows = toUpperSet(request.getSweetboxRows());

        List<Seat> toSave = new ArrayList<>();
        for (int r = 0; r < request.getRows(); r++) {
            String rowLabel = String.valueOf((char) ('A' + r));

            SeatType type = SeatType.STANDARD;
            if (sweetboxRows.contains(rowLabel)) {
                type = SeatType.SWEETBOX;
            } else if (vipRows.contains(rowLabel)) {
                type = SeatType.VIP;
            }

            for (int n = 1; n <= request.getSeatsPerRow(); n++) {
                String seatName = rowLabel + n;
                if (seatRepository.existsByRoomIdAndSeatName(roomId, seatName)) {
                    continue;
                }
                Seat seat = new Seat();
                seat.setSeatName(seatName);
                seat.setSeatType(type);
                seat.setRoom(room);
                toSave.add(seat);
            }
        }

        List<Seat> saved = seatRepository.saveAll(toSave);
        clearScreenSeatsCache(roomId);
        return saved;
    }

    private Room findRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Phòng chiếu"));
    }

    private Set<String> toUpperSet(List<String> rows) {
        Set<String> result = new HashSet<>();
        if (rows != null) {
            for (String row : rows) {
                if (row != null) {
                    result.add(row.trim().toUpperCase());
                }
            }
        }
        return result;
    }

    // GHI: Khi Admin thêm/sửa/xóa sơ đồ ghế của phòng, phải xóa cache này
    public void clearScreenSeatsCache(Long roomId) {
        redisTemplate.delete("screen:" + roomId + ":seats");
    }
}
