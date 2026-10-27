# Yêu cầu bảo mật — Hệ thống đặt vé xem phim

## 1. Phạm vi
Hệ thống gồm 5 service Spring Boot: `user-service` (8081), `movie-service` (8082), `venue-service` (8083), `booking-service` (8084), `notification-service` (8085). Dữ liệu nằm ở MySQL, khóa giữ ghế ở Redis, thông báo qua RabbitMQ.

## 2. Tài sản cần bảo vệ
| Tài sản | Rủi ro | Biện pháp |
|---|---|---|
| Tài khoản và mật khẩu | Lộ mật khẩu nếu DB bị đọc | Băm bằng BCrypt, không lưu plain-text |
| Vé và đơn đặt của khách | Khách này xem hoặc hủy vé khách khác | Kiểm tra quyền sở hữu (Resource Ownership), trả 403 |
| Dữ liệu quản trị (phim, suất chiếu, rạp, phòng, ghế) | Khách thường sửa dữ liệu | `@PreAuthorize("hasRole('ADMIN')")` cho API ghi |
| Ghế đã giữ / đã bán | Bán trùng ghế, giữ ghế vô hạn | Khóa Redis có TTL + ràng buộc unique `seat_claims` trong SQL |
| Khóa ký JWT (`JWT_SECRET`) | Giả mạo token | Đọc từ biến môi trường, tối thiểu 32 byte, không ghi trong source |

## 3. Yêu cầu xác thực (Authentication)
1. Đăng ký `POST /api/v1/auth/register`: username duy nhất, mật khẩu được băm BCrypt, vai trò mặc định `CUSTOMER`.
2. Đăng nhập `POST /api/v1/auth/login`: đúng thì cấp Access Token (JWT), sai thông tin hoặc tài khoản bị khóa thì trả **401**.
3. JWT ký HMAC bằng khóa bí mật dùng chung các service, chứa `sub` (username), `userId`, `role`.
4. **Hạn Access Token 15 phút** (cấu hình bằng `jwt.expiration-minutes`, mặc định 15, cho phép 1–1440).
5. Mọi API (trừ danh sách công khai ở mục 5) yêu cầu header `Authorization: Bearer <token>`.
6. Không có token, token sai chữ ký hoặc token hết hạn đều trả **401** với JSON lỗi chuẩn.
7. Tài khoản `active = false` không đăng nhập được.

## 4. Yêu cầu phân quyền (Authorization)
- Vai trò đã cài đặt: `CUSTOMER`, `ADMIN` (xem `role-permission-matrix.md`).
- Phân quyền theo chức năng bằng `@PreAuthorize` ở controller.
- **Resource Ownership**: khách chỉ xem, thanh toán, hủy vé của chính mình. Xem vé người khác trả **403**. Admin được xem mọi vé.
- Admin không được tự đặt vé sang `PAID` bằng API quản trị (tránh phát hành vé không có thanh toán). Thanh toán phải đi qua `POST /api/v1/tickets/{id}/pay` của chủ vé.
- Vé `PAID` hoặc `PENDING` không bị xóa bằng API quản trị.
- Điều kiện nghiệp vụ khi hủy: chỉ hủy được vé `PENDING`. Vé đã `PAID`, `CANCELLED`, `EXPIRED` hủy sẽ trả **409**.

## 5. API công khai (không cần token)
`GET /api/v1/movies/**`, `GET /api/v1/showtimes/**`, `GET /api/v1/cinemas`, `GET /api/v1/rooms/{id}/seats`, `GET /api/v1/seats/**`, `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, Swagger (`/swagger-ui/**`, `/v3/api-docs/**`).

## 6. Mã lỗi bảo mật và định dạng
| Tình huống | Mã | Ghi chú |
|---|---|---|
| Không có token / token sai / token hết hạn | 401 | `RestAuthenticationEntryPoint` |
| Sai username hoặc mật khẩu | 401 | |
| Đã đăng nhập nhưng không đủ quyền | 403 | `RestAccessDeniedHandler` và `GlobalExceptionHandler` |
| Vé của người khác | 403 | Kiểm tra sở hữu |
| Dữ liệu sai | 400 | Bean Validation, JSON sai định dạng |

Mọi lỗi trả cùng cấu trúc:
```json
{ "timestamp": "2026-10-25T10:00:00", "status": 403, "error": "Forbidden", "message": "...", "path": "/api/v1/tickets/5" }
```

## 7. JWT Filter dùng chung
Lớp `JwtAuthenticationFilter` và `JwtUtil` nằm trong module `common`, được các service `user`, `movie`, `venue`, `booking` dùng chung. Filter đọc token, kiểm tra chữ ký và hạn, rồi đặt `ROLE_<role>` và `userId` vào request.

## 8. Cấu hình và bí mật
- `JWT_SECRET`, `DB_PASSWORD`, `RABBITMQ_PASSWORD` lấy từ biến môi trường ở môi trường thật. Profile `dev` chỉ dùng trên máy cá nhân.
- Không có tài khoản admin mặc định. Admin tạo qua `BOOTSTRAP_ADMIN_*` với mật khẩu từ 12 ký tự.

## 9. Bảo mật giữa các service (Service-to-Service)
Hiện trạng và hướng nâng cấp:
1. **Hiện trạng**: `booking-service` gọi `movie-service` và `venue-service` bằng các API `GET` công khai (chỉ đọc), có timeout 2s/3s, retry 3 lần, và trả 503 khi không gọi được. Không có thông tin nhạy cảm nào đi qua các lời gọi này.
2. **Truyền JWT nguyên trạng**: chuyển tiếp token của khách sang service đích. Đơn giản, nhưng service đích phải tin token của khách và token có thể hết hạn giữa chừng.
3. **OAuth2 Client Credentials**: mỗi service có `client_id`/`client_secret`, xin token riêng với scope (ví dụ `showtime.read`) từ máy chủ cấp phát. Token do máy, không gắn với khách. Phù hợp khi service đích cần chặn lời gọi ẩn danh.
4. **Đề xuất cho dự án**: giữ cách 1 cho API chỉ đọc. Khi có `payment-service` gọi ghi vào booking, dùng token role `SERVICE` hoặc Client Credentials cho endpoint chốt vé và đặt mạng nội bộ cho các cổng 8081–8085.

## 10. Việc chưa làm
- Vai trò `STAFF` chưa cài đặt.
- Chưa có thu hồi token (revoke) hay refresh token: token hết hạn sau 15 phút, khách đăng nhập lại.
- Chưa giới hạn tần suất đăng nhập (rate limit).
