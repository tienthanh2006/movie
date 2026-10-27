# Báo cáo kiểm thử (test-results.md)

> **Trạng thái: CHƯA CHẠY.** Các cột "Thực tế" và "Kết quả" để trống vì bộ Postman chưa được chạy trên hệ thống thật
> (cần Docker, Maven, MySQL, Redis, RabbitMQ). Sau khi chạy, điền kết quả (Đạt / Không đạt) và đính kèm ảnh chụp
> response status/body như đề bài yêu cầu. Không điền "Đạt" nếu chưa chạy thật.

## 1. Môi trường thử
| Mục | Giá trị |
|---|---|
| Ngày chạy | |
| Người chạy | |
| Cách khởi động | `run-dev.ps1` (profile `dev`) |
| Công cụ | Postman (Collection Runner) |
| Phiên bản commit/zip | |

## 2. Kiểm tra đã chạy được khi soạn bản này (không cần hạ tầng)
Chạy bằng JDK 21, không có Maven nên **chưa** chạy `mvn verify`:

| Kiểm tra | Kết quả |
|---|---|
| `tests/offline/OfflineRulesTest.java` (quy tắc giá, giờ chiếu, phòng, hết hạn) | PASS 20 khẳng định |
| `tests/offline/ResilienceOfflineTest.java` (retry, fallback, Idempotency-Key, trạng thái ghế) | PASS 11 khẳng định |
| `tests/offline/ParseAllJava.java` (cú pháp 87 file Java) | PASS (chỉ kiểm tra cú pháp, chưa dò dependency) |
| JUnit (`TicketRulesTest`, `ShowtimeRulesTest`, `RetryExecutorTest`) | Chưa chạy (cần Maven) |
| Tích hợp MySQL/Redis/RabbitMQ, 2 khách đặt cùng ghế | Chưa chạy |

## 3. Cách chạy Postman
1. `.\run-dev.ps1`, chờ 5 service lên.
2. Import `postman/Cinema_System_V3.postman_collection.json` và `postman/Cinema_Booking_V5_Security.postman_collection.json`.
3. Chạy từng collection bằng Collection Runner, giữ nguyên thứ tự. Thư mục "0. Chuẩn bị" tự tạo dữ liệu và token.
4. Hai request cần làm tay: tài khoản bị khóa (biến `lockedUser`) và token hết hạn (biến `expiredToken`, dùng token cũ hơn 15 phút hoặc đặt `JWT_EXPIRATION_MINUTES`/`jwt.expiration-minutes=1` rồi chờ).

## 4. Ca kiểm thử Cinema_System_V3 (REST, validation, idempotency)

### 0. Chuẩn bị (đăng nhập, tạo dữ liệu)

| # | Request | Mong đợi (HTTP) | Thực tế | Kết quả |
|---|---|---|---|---|
| 1 | POST — Đăng nhập ADMIN | 200 |  |  |
| 2 | POST — Đăng ký CUSTOMER (username ngẫu nhiên) | 200 |  |  |
| 3 | POST — Đăng nhập CUSTOMER | 200 |  |  |
| 4 | POST — Admin tạo phim | 201 |  |  |
| 5 | POST — Admin tạo rạp | 201 |  |  |
| 6 | POST — Admin tạo phòng | 201 |  |  |
| 7 | POST — Admin tạo ghế hàng loạt (A1-A5, B1-B5) | 201 |  |  |
| 8 | GET — Lấy ghế của phòng (lưu 3 id đầu) | 200 |  |  |
| 9 | POST — Admin tạo suất chiếu (ngày ngẫu nhiên trong tương lai) | 201 |  |  |

### 1. Movie — CRUD + Search/Filter/Sort/Pagination

| # | Request | Mong đợi (HTTP) | Thực tế | Kết quả |
|---|---|---|---|---|
| 10 | GET — GET danh sách phim, phân trang + sắp xếp | 200 |  |  |
| 11 | GET — Tìm theo tiêu đề | 200 |  |  |
| 12 | GET — Lọc theo thể loại | 200 |  |  |
| 13 | GET — GET phim đang chiếu (cache Redis) | 200 |  |  |
| 14 | GET — GET phim sắp chiếu | 200 |  |  |
| 15 | GET — GET phim theo id | 200 |  |  |
| 16 | GET — GET phim không tồn tại -> 404 | 404 |  |  |
| 17 | POST — POST phim thiếu tiêu đề -> 400 | 400 |  |  |
| 18 | POST — POST phim năm phát hành sai -> 400 | 400 |  |  |
| 19 | POST — POST JSON sai định dạng -> 400 | 400 |  |  |
| 20 | PUT — PUT cập nhật phim | 200 |  |  |

### 2. Venue — Rạp, phòng, ghế (có PATCH)

| # | Request | Mong đợi (HTTP) | Thực tế | Kết quả |
|---|---|---|---|---|
| 21 | GET — GET danh sách rạp | 200 |  |  |
| 22 | POST — POST phòng thiếu tên -> 400 | 400 |  |  |
| 23 | PATCH — PATCH ghế: giữ ghế (status=HELD) | 200 |  |  |
| 24 | PATCH — PATCH ghế: nhả ghế (status=AVAILABLE) | 200 |  |  |
| 25 | PATCH — PATCH ghế: đổi loại ghế (seatType=VIP) | 200 |  |  |
| 26 | PATCH — PATCH ghế: body rỗng -> 400 | 400 |  |  |
| 27 | PATCH — PATCH ghế: status sai giá trị -> 400 | 400 |  |  |
| 28 | PATCH — PATCH ghế không tồn tại -> 404 | 404 |  |  |

### 3. Showtime

| # | Request | Mong đợi (HTTP) | Thực tế | Kết quả |
|---|---|---|---|---|
| 29 | GET — GET suất chiếu theo phim + ngày | 200 |  |  |
| 30 | GET — GET suất chiếu theo id | 200 |  |  |
| 31 | GET — GET suất chiếu không tồn tại -> 404 | 404 |  |  |
| 32 | POST — POST suất chiếu trùng giờ phòng -> 409 | 409 |  |  |
| 33 | POST — POST suất chiếu giá âm -> 400 | 400 |  |  |

### 4. Booking — Status code, Nested resource, Idempotency

| # | Request | Mong đợi (HTTP) | Thực tế | Kết quả |
|---|---|---|---|---|
| 34 | POST — Đặt vé (có Idempotency-Key) -> 201 | 201 |  |  |
| 35 | POST — Gửi lại CÙNG Idempotency-Key -> 200, trả lại vé cũ | 200 |  |  |
| 36 | POST — CÙNG Idempotency-Key nhưng ghế khác -> 409 | 409 |  |  |
| 37 | POST — Idempotency-Key sai định dạng -> 400 | 400 |  |  |
| 38 | POST — Đặt trùng ghế đang giữ (không key) -> 409 | 409 |  |  |
| 39 | POST — Đặt vé thiếu seatId -> 400 | 400 |  |  |
| 40 | POST — Đặt vé suất chiếu không tồn tại -> 404 | 404 |  |  |
| 41 | PATCH — Giữ ghế thủ công rồi đặt -> 409 (ghế HELD) | 200 |  |  |
| 42 | POST — Đặt ghế đang HELD -> 409 | 409 |  |  |
| 43 | PATCH — Nhả ghế lại (AVAILABLE) | 200 |  |  |
| 44 | GET — GET vé của tôi | 200 |  |  |
| 45 | GET — GET chi tiết vé | 200 |  |  |
| 46 | GET — GET vé không tồn tại -> 404 | 404 |  |  |
| 47 | POST — Thanh toán giả lập -> 200 PAID | 200 |  |  |
| 48 | POST — Thanh toán lần 2 -> 409 | 409 |  |  |
| 49 | POST — Đặt ghế khác để thử hủy | 201 |  |  |
| 50 | POST — Hủy vé PENDING -> 200 CANCELLED | 200 |  |  |
| 51 | POST — Hủy lần 2 -> 409 | 409 |  |  |
| 52 | POST — Đặt lại đúng ghế vừa hủy -> 201 (ghế đã được nhả) | 201 |  |  |
| 53 | GET — Nested resource: GET /users/{userId}/tickets (chủ vé) | 200 |  |  |


## 5. Ca kiểm thử Cinema_Booking_V5_Security (bảo mật)

### 0. Chuẩn bị

| # | Request | Mong đợi (HTTP) | Thực tế | Kết quả |
|---|---|---|---|---|
| 1 | POST — Admin đăng nhập | 200 |  |  |
| 2 | POST — Đăng ký khách A | 200 |  |  |
| 3 | POST — Khách A đăng nhập | 200 |  |  |
| 4 | POST — Đăng ký khách B | 200 |  |  |
| 5 | POST — Khách B đăng nhập | 200 |  |  |
| 6 | POST — Admin tạo phim | 201 |  |  |
| 7 | POST — Admin tạo rạp | 201 |  |  |
| 8 | POST — Admin tạo phòng | 201 |  |  |
| 9 | POST — Admin tạo ghế hàng loạt (A1-A5, B1-B5) | 201 |  |  |
| 10 | GET — Lấy ghế của phòng (lưu 3 id đầu) | 200 |  |  |
| 11 | POST — Admin tạo suất chiếu (ngày ngẫu nhiên trong tương lai) | 201 |  |  |
| 12 | POST — Khách A đặt vé (tạo vé của A) | 201 |  |  |

### 1. Đăng nhập (401 / 400)

| # | Request | Mong đợi (HTTP) | Thực tế | Kết quả |
|---|---|---|---|---|
| 13 | POST — Sai mật khẩu -> 401 | 401 |  |  |
| 14 | POST — Tài khoản không tồn tại -> 401 | 401 |  |  |
| 15 | POST — Thiếu mật khẩu -> 400 | 400 |  |  |
| 16 | POST — Tài khoản bị khóa -> 401 (làm tay: đặt active=false trong user_db rồi đăng nhập) | 401 (làm tay, tự bỏ qua nếu thiếu biến) |  |  |

### 2. Chưa đăng nhập hoặc token sai -> 401

| # | Request | Mong đợi (HTTP) | Thực tế | Kết quả |
|---|---|---|---|---|
| 17 | POST — Không token: đặt vé | 401 |  |  |
| 18 | GET — Không token: vé của tôi | 401 |  |  |
| 19 | POST — Không token: thêm phim | 401 |  |  |
| 20 | POST — Token rác: đặt vé | 401 |  |  |
| 21 | GET — Token sai chữ ký (đổi 1 ký tự giữa phần chữ ký): xem vé | — |  |  |
| 22 | GET — Token HẾT HẠN -> 401 (làm tay: đặt biến expiredToken bằng token cũ quá 15 phút) | 401 (làm tay, tự bỏ qua nếu thiếu biến) |  |  |

### 3. Sai vai trò -> 403 (khách thường dùng API admin)

| # | Request | Mong đợi (HTTP) | Thực tế | Kết quả |
|---|---|---|---|---|
| 23 | POST — Khách: thêm phim | 403 |  |  |
| 24 | DELETE — Khách: xóa phim | 403 |  |  |
| 25 | POST — Khách: thêm suất chiếu | 403 |  |  |
| 26 | POST — Khách: thêm rạp | 403 |  |  |
| 27 | PATCH — Khách: PATCH ghế | 403 |  |  |
| 28 | GET — Khách: xem toàn bộ vé | 403 |  |  |
| 29 | DELETE — Khách: xóa vé | 403 |  |  |
| 30 | GET — Admin: xem toàn bộ vé -> 200 | 200 |  |  |
| 31 | PATCH — Admin: PATCH ghế -> 200 | 200 |  |  |

### 4. Quyền sở hữu vé (Resource Ownership)

| # | Request | Mong đợi (HTTP) | Thực tế | Kết quả |
|---|---|---|---|---|
| 32 | GET — Chủ vé (A) xem vé của mình -> 200 | 200 |  |  |
| 33 | GET — Khách B xem vé của A -> 403 | 403 |  |  |
| 34 | GET — Khách B xem danh sách vé theo id của A -> 403 | 403 |  |  |
| 35 | POST — Khách B thanh toán vé của A -> 403 | 403 |  |  |
| 36 | POST — Khách B hủy vé của A -> 403 | 403 |  |  |
| 37 | GET — Admin xem vé của A -> 200 | 200 |  |  |
| 38 | POST — Admin thanh toán vé của A -> 403 (chỉ chủ vé được trả tiền) | 403 |  |  |

### 5. Điều kiện nghiệp vụ

| # | Request | Mong đợi (HTTP) | Thực tế | Kết quả |
|---|---|---|---|---|
| 39 | PATCH — Admin đặt PAID bằng PATCH -> 409 | 409 |  |  |
| 40 | DELETE — Admin xóa vé PENDING -> 409 | 409 |  |  |
| 41 | POST — Chủ vé (A) thanh toán -> 200 PAID | 200 |  |  |
| 42 | POST — Chủ vé hủy vé đã PAID -> 409 | 409 |  |  |
| 43 | DELETE — Admin xóa vé PAID -> 409 | 409 |  |  |

### 6. API công khai không cần token -> 200

| # | Request | Mong đợi (HTTP) | Thực tế | Kết quả |
|---|---|---|---|---|
| 44 | GET — GET phim | 200 |  |  |
| 45 | GET — GET rạp | 200 |  |  |
| 46 | GET — GET ghế của phòng | 200 |  |  |


## 6. Ca kiểm thử làm tay (không có trong Postman)
| Ca | Cách làm | Mong đợi | Kết quả |
|---|---|---|---|
| Hai trình duyệt cùng chọn một ghế | 2 khách gọi POST /tickets cùng lúc cùng ghế | 1 khách 201, khách kia 409 | |
| Hết thời gian giữ ghế | Đặt `SEAT_HOLD_SECONDS=60`, đặt vé, chờ >1 phút, thanh toán | 410, vé chuyển EXPIRED sau tối đa 1 phút (job dọn), ghế đặt lại được | |
| Tắt Redis khi đang chạy | `docker stop cinema-redis`, gọi POST /tickets | Không đặt được vé (lỗi), API đọc phim vẫn trả dữ liệu từ MySQL | |
| Tắt movie-service | Dừng movie-service, gọi POST /tickets | 503 sau khi retry (xem log "không phản hồi sau 3 lần thử") | |
| Gọi chậm quá timeout | Cho movie-service ngủ >3s | 503 | |
| Bán trùng ghế khi khóa Redis hết hạn | Xóa khóa Redis tay giữa lúc 2 người đặt | Ràng buộc unique `seat_claims` vẫn chặn, trả 409 | |
