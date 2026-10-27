# Cinema System (microservices)

| Service | Cổng | Database |
|---|---|---|
| user-service | 8081 | user_db |
| movie-service | 8082 | movie_db |
| venue-service (rạp, phòng, ghế) | 8083 | venue_db |
| booking-service (đặt vé, thanh toán giả lập) | 8084 | booking_db |
| notification-service | 8085 | không |

`common` là thư viện dùng chung: JWT, xử lý lỗi tập trung, 401/403 dạng JSON, retry, định dạng tin nhắn RabbitMQ.

## Chạy nhanh trên máy cá nhân (Windows)
Cần: Docker Desktop, JDK 17+, Maven (hoặc `mvnw.cmd` đã copy vào thư mục này).

    .\run-dev.ps1

Script bật MySQL, Redis, RabbitMQ, build, rồi mở 5 cửa sổ chạy 5 service với profile `dev`
(`SPRING_PROFILES_ACTIVE=dev`). Profile `dev` tự: tạo bảng (`ddl-auto=update`), tạo admin
`admin / Admin@123456`, tạo dữ liệu mẫu, dùng mật khẩu DB/RabbitMQ của docker-compose.
**Chỉ dùng cho máy cá nhân.** Khi triển khai thật phải đặt `JWT_SECRET`, `DB_USER`, `DB_PASSWORD`,
`RABBITMQ_USER`, `RABBITMQ_PASSWORD` bằng biến môi trường và chạy migration (xem `migrations/README.md`).

Nếu trước đó đã có database cũ, xóa sạch để tạo lại: `docker compose down -v` rồi chạy lại.

Chạy tay từng service (mỗi lệnh một terminal):

    $env:SPRING_PROFILES_ACTIVE="dev"
    mvn -pl user-service spring-boot:run     # tương tự movie-service, venue-service, booking-service, notification-service

Swagger: http://localhost:8082/swagger-ui/index.html (movie), 8083 (venue), 8084 (booking).

## Thay đổi so với bản trước
- **JWT hạn 15 phút** (`jwt.expiration-minutes`, mặc định 15).
- **401 cho chưa đăng nhập / token sai / hết hạn**, 403 cho thiếu quyền, cùng cấu trúc JSON lỗi (timestamp, status, error, message, path).
- **Gọi movie/venue có timeout (2s/3s), retry 3 lần, fallback trả 503** khi service kia không phản hồi. 404 giữ nguyên là 404.
- JSON sai định dạng, enum sai giá trị, sai kiểu tham số trả **400** (trước đây có thể rơi vào 500).
- **PATCH /api/v1/seats/{id}** (ADMIN): cập nhật một phần ghế. Body ví dụ `{"status":"HELD"}`, `{"status":"AVAILABLE"}`, `{"seatType":"VIP"}`.
  Trạng thái ghế: `AVAILABLE`, `HELD`, `MAINTENANCE`. Ghế khác `AVAILABLE` không đặt vé được (409).
- **Idempotency-Key** cho `POST /api/v1/tickets` (header tùy chọn). Gửi lại cùng key và cùng ghế thì trả lại vé cũ (200,
  header `Idempotent-Replayed: true`). Cùng key nhưng khác ghế hoặc suất thì 409. Key sai định dạng thì 400.
  Nếu hai yêu cầu cùng key đến đồng thời, yêu cầu đến sau có thể nhận 409; gửi lại sẽ nhận vé gốc.

## API chính
| Phương thức | Đường dẫn | Cổng | Quyền |
|---|---|---|---|
| POST | /api/v1/auth/register, /api/v1/auth/login | 8081 | công khai |
| GET | /api/v1/movies (title, genre, page, size, sort), /movies/{id}, /movies/now-showing, /movies/coming-soon | 8082 | công khai |
| POST, PUT, DELETE | /api/v1/movies, /api/v1/showtimes | 8082 | ADMIN |
| GET | /api/v1/showtimes?movieId=&date=, /api/v1/showtimes/{id} | 8082 | công khai |
| GET | /api/v1/cinemas, /api/v1/rooms/{id}/seats, /api/v1/seats/{id} | 8083 | công khai |
| POST, DELETE | /api/v1/cinemas | 8083 | ADMIN |
| POST, PUT, DELETE | /api/v1/rooms, /api/v1/seats; POST /api/v1/rooms/{id}/seats/generate | 8083 | ADMIN |
| **PATCH** | /api/v1/seats/{id} | 8083 | ADMIN |
| POST | /api/v1/tickets (Idempotency-Key tùy chọn) | 8084 | đăng nhập |
| GET | /api/v1/tickets/my-tickets, /tickets/{id}, /users/{userId}/tickets | 8084 | chủ vé (admin xem được) |
| POST | /api/v1/tickets/{id}/pay, /tickets/{id}/cancel | 8084 | chủ vé |
| GET, PATCH, DELETE | /api/v1/tickets, /tickets/{id}/status, /tickets/{id} | 8084 | ADMIN |

Chi tiết phân quyền: `docs/role-permission-matrix.md`. Yêu cầu bảo mật: `docs/security-requirements.md`.

## Test
- Postman: `postman/Cinema_System_V3.postman_collection.json` (REST) và `postman/Cinema_Booking_V5_Security.postman_collection.json` (bảo mật).
  Import rồi chạy bằng Collection Runner. Mẫu báo cáo: `docs/test-results.md` (điền kết quả sau khi chạy thật).
- Không cần hạ tầng: `tests/offline/` (xem `VALIDATION.md`). Cần Maven: `mvn -B clean verify`.

## Tài liệu
`docs/security-requirements.md`, `docs/role-permission-matrix.md`, `docs/framework-comparison.md`, `docs/test-results.md`.

## Chưa làm
Đặt nhiều ghế trong một đơn, Cloudflare R2, frontend, vai trò STAFF, payment-service riêng, API v2, thu hồi token.
`VALIDATION.md` và `CHANGELOG_FIXES.md` ghi rõ những gì chưa được kiểm chứng.
