# Báo cáo thay đổi — Cinema System (25/10/2026)

**Nguồn:** `cinema-system.zip` người dùng cung cấp; mọi chỉnh sửa thực hiện trên **bản sao**, không đụng file/DB/service trên máy Windows.

## Các mã lỗi từ báo cáo trước và trạng thái bản vá

| Mã | Thay đổi trong mã nguồn | Mức xác minh |
|---|---|---|
| SEC-01 | Bỏ seed ADMIN/123456 mặc định; bootstrap chỉ khi bật và cấp mật khẩu mạnh | Tĩnh + parse Java; chưa khởi chạy |
| SEC-02 | Bắt buộc JWT_SECRET (>=32 byte), bỏ khóa dự phòng | Tĩnh + parse Java |
| SEC-03 | Áp dụng `disabled(!active)` ở UserDetailsService | Tĩnh + parse Java; token cũ cần chiến lược thu hồi |
| BOOK-01 | `expiresAt`, `EXPIRED`, cron dọn và giải phóng Redis/SQL | Logic code; chưa thử Redis/MySQL |
| BOOK-02 | Kiểm tra seat.room.id trùng showtime.roomId | 20 bài kiểm tra quy tắc Java + tĩnh |
| BOOK-03 | Xác minh suất OPEN và trước giờ chiếu khi đặt lẫn thanh toán | Quy tắc Java + tĩnh |
| BOOK-04 | Token giữ ghế riêng, xóa Redis bằng Lua compare-and-delete | Tĩnh; chưa chạy Redis đồng thời |
| BOOK-05 | Thêm `seat_claims` unique `(showtime_id, seat_id)` trong SQL | Tĩnh; cần chạy migration & concurrency test |
| BOOK-06 | Thêm `/api/v1/tickets/{id}/pay`, chỉ chủ vé; cấm ADMIN PATCH sang PAID | Tĩnh; chưa integration test |
| BOOK-07 | Hủy/hết hạn giải phóng quyền giữ và khóa Redis sau SQL commit, hạn chế DELETE | Tĩnh; chưa integration test |
| MOV-01 | Khóa DB theo phòng qua `room_schedule_mutex`, giao dịch có thứ tự | Tĩnh; chưa thử đồng thời |
| MOV-02 | Tự tính end = start + durationMinutes + 15 phút, xử lý qua nửa đêm | Kiểm tra quy tắc JVM + tĩnh |
| OPS-01 | DB/RabbitMQ secret lấy qua biến môi trường; không chứa mật khẩu mặc định | Tĩnh |
| QA-01 | Thêm 2 bộ JUnit và 20 bài kiểm tra Java chạy độc lập | Java offline PASS; Maven/JUnit chưa chạy |
| API-01 | Tranh chấp ghế đi qua `ConflictException` thành HTTP 409 | Tĩnh + giữ nguyên routes |

## Kết quả kiểm tra đã thực thi

- PASS 20 bài khẳng định nghiệp vụ với `javac`/`java` JDK 21, không cần dependency.
- PASS parser `javac` cho 77 file Java (gồm source/test); **chỉ kiểm tra cú pháp**, không resolve Spring dependencies.
- PASS đối chiếu 31 route gốc, cùng verb + URL; không thay đổi `/api/v1`; bổ sung 2 route mới.
- PASS kiểm tra cấu trúc nguồn và đặc điểm bản vá.
- **KHÔNG chạy được** `mvn clean verify`: không có Maven trong môi trường thực thi, không có khả năng tải dependency.
- **KHÔNG chạy được** integration test MySQL/Redis/RabbitMQ hoặc stress test 2 khách đồng thời.

## Những phần chưa hoàn thành so với PDF

- Một đơn chứa nhiều ghế/khóa nhiều ghế bằng Lua nguyên tử; hiện vẫn là một vé cho một ghế.
- Cloudflare R2 presigned upload và frontend khách/Admin (không có trong nguồn được cung cấp).
- Chưa chuyển đổi hệ dữ liệu sang `bookings`, `booking_seats`, `showtime_seats`, bảng phụ thu `seat_types` theo PDF.
- Khác biệt username/CUSTOMER so với email/USER theo PDF vẫn được giữ để không phá API/DB cũ.
- JWT cấp trước khi khóa tài khoản có thể tiếp tục hợp lệ đến hết hạn (cần giải pháp revoke/token version).
- Cần đánh giá cụ thể DB schema hiện tại và kế hoạch migration trước khi triển khai.

## Lưu ý triển khai

1. Sao lưu CSDL và source hiện hành; tuyệt đối không chạy schema script trên production mà chưa dry-run.
2. Chạy `migrations/preflight.sql` rồi `migrations/schema_update_mysql8.sql` trên bản thử nghiệm.
3. Thiết lập JWT_SECRET, DB_PASSWORD, RABBITMQ_PASSWORD; **không copy lại secret cũ**.
4. Trên máy có Maven: chạy `mvn -B clean verify`, khởi động các service, kiểm tra phân quyền, hết hạn, thanh toán và giao dịch đồng thời.
5. Sau khi kiểm thử đạt mới thay code chính thức; giữ ZIP gốc làm rollback.

**Không được tuyên bố bản vá đã sẵn sàng production khi chưa thực hiện build/integration.**


---
# Vòng sửa tiếp theo (cấu hình chạy, JWT 15 phút/401, timeout-retry-fallback/503, PATCH ghế, Idempotency-Key, tài liệu)

| Hạng mục | Thay đổi | Mức xác minh |
|---|---|---|
| Chạy được ngay | `docker-compose.yml` tạo user DB `cinema` và user RabbitMQ `cinema`; `init-db.sql` cấp quyền; `application-dev.properties` cho 5 service; `run-dev.ps1` | Tĩnh; chưa chạy Docker |
| JWT 15 phút | `JwtUtil` đọc `jwt.expiration-minutes` (mặc định 15) | Tĩnh |
| 401 chưa đăng nhập | `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler` trong `common`, gắn vào 4 `SecurityConfig` | Tĩnh |
| Lỗi 400/503 | `GlobalExceptionHandler` thêm 503, JSON sai định dạng/kiểu tham số/thiếu header -> 400 | Tĩnh |
| Timeout/retry/fallback | `RemoteCaller` + `RetryExecutor`; lỗi mạng/timeout/5xx retry 3 lần rồi 503; 404 không retry | `RetryExecutor` chạy offline PASS; phần Spring chỉ đọc code |
| PATCH ghế | `PATCH /api/v1/seats/{id}`, thêm `Seat.status`; booking từ chối ghế khác AVAILABLE | Tĩnh |
| Idempotency-Key | Header trên `POST /tickets`, cột `tickets.idempotency_key` + unique `(user_id, idempotency_key)` | Quy tắc chạy offline PASS; luồng DB chưa chạy |
| Enum | `Ticket.status`, `Seat.status` lưu VARCHAR (`@JdbcTypeCode`), migration đổi cột `status` sang VARCHAR | Tĩnh; cần thử trên MySQL thật |
| Tài liệu | `docs/` (4 file) và `postman/` (2 collection) | JSON hợp lệ; chưa chạy trên Postman |

Kiểm tra đã chạy: `ResilienceOfflineTest` PASS 11, `OfflineRulesTest` PASS 20, parse 87 file Java PASS, `check_source.py` PASS.
**Chưa chạy**: `mvn verify`, MySQL/Redis/RabbitMQ, Postman.
