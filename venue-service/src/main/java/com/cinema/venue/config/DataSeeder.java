package com.cinema.venue.config;

import com.cinema.venue.model.Cinema;
import com.cinema.venue.model.Room;
import com.cinema.venue.model.Seat;
import com.cinema.venue.model.SeatType;
import com.cinema.venue.repository.CinemaRepository;
import com.cinema.venue.repository.RoomRepository;
import com.cinema.venue.repository.SeatRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

// Thay cho TestController cũ: tự tạo dữ liệu mẫu lần chạy đầu tiên
@Component
@ConditionalOnProperty(name = "cinema.demo-data.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final CinemaRepository cinemaRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;

    public DataSeeder(CinemaRepository cinemaRepository, RoomRepository roomRepository, SeatRepository seatRepository) {
        this.cinemaRepository = cinemaRepository;
        this.roomRepository = roomRepository;
        this.seatRepository = seatRepository;
    }

    @Override
    public void run(String... args) {
        if (cinemaRepository.count() > 0) {
            return;
        }

        Cinema cinema = new Cinema();
        cinema.setName("CGV Vincom");
        cinema.setAddress("Hà Nội");
        cinemaRepository.save(cinema);

        Room room = new Room();
        room.setName("Phòng 1");
        room.setCapacity(50);
        room.setCinema(cinema);
        roomRepository.save(room);

        Seat seat1 = new Seat();
        seat1.setSeatName("A1");
        seat1.setSeatType(SeatType.STANDARD);
        seat1.setRoom(room);

        Seat seat2 = new Seat();
        seat2.setSeatName("VIP1");
        seat2.setSeatType(SeatType.VIP);
        seat2.setRoom(room);

        seatRepository.save(seat1);
        seatRepository.save(seat2);

        System.out.println("====== VENUE-SERVICE: ĐÃ TẠO DỮ LIỆU MẪU (ghế thường id " + seat1.getId() + ", ghế VIP id " + seat2.getId() + ") ======");
    }
}
