package com.cinema.venue.service;

import com.cinema.common.exception.ResourceNotFoundException;
import com.cinema.venue.dto.RoomRequest;
import com.cinema.venue.model.Cinema;
import com.cinema.venue.model.Room;
import com.cinema.venue.repository.CinemaRepository;
import com.cinema.venue.repository.RoomRepository;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final CinemaRepository cinemaRepository;
    private final SeatService seatService;

    public RoomService(RoomRepository roomRepository, CinemaRepository cinemaRepository, SeatService seatService) {
        this.roomRepository = roomRepository;
        this.cinemaRepository = cinemaRepository;
        this.seatService = seatService;
    }

    public Room createRoom(RoomRequest request) {
        Cinema cinema = findCinema(request.getCinemaId());

        Room room = new Room();
        room.setName(request.getName());
        room.setCapacity(request.getCapacity());
        room.setCinema(cinema);
        return roomRepository.save(room);
    }

    public Room updateRoom(Long id, RoomRequest request) {
        Room room = findRoom(id);
        Cinema cinema = findCinema(request.getCinemaId());

        room.setName(request.getName());
        room.setCapacity(request.getCapacity());
        room.setCinema(cinema);
        return roomRepository.save(room);
    }

    // Xóa phòng sẽ xóa luôn các ghế của phòng (cascade)
    public void deleteRoom(Long id) {
        Room room = findRoom(id);
        roomRepository.delete(room);
        seatService.clearScreenSeatsCache(id);
    }

    private Room findRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Phòng chiếu"));
    }

    private Cinema findCinema(Long id) {
        return cinemaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Rạp"));
    }
}
