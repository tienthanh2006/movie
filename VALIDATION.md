# Xác minh bản vá — Cinema System

## Phạm vi
- Giữ nguyên **tất cả tiền tố `/api/v1`**. Không xóa các route cũ.
- Sửa nhóm lỗi bảo mật, giữ ghế/đặt vé đơn, thanh toán giả lập, hết hạn, lịch chiếu.
- Mã nguồn gốc trong ZIP không bị ghi đè. Đây là bản sao phát hành để review.

## Những gì có thể thử ngay
- `tests/offline/OfflineRulesTest.java`: bài kiểm thử Java 17 thuần (không cần Maven/DB).
- `tests/offline/check_source.py`: kiểm tra các route `/api/v1` và sự hiện diện của chốt an toàn.

## Cần xác minh trên máy có môi trường đầy đủ
- `mvn -B clean verify` ở thư mục gốc sau khi cấp biến môi trường.
- Triển khai schema MySQL 8 trên bản staging, chạy các service trong mạng cách ly.
- Kiểm thử đồng thời 2 luồng: cùng chọn ghế, thanh toán sát giờ hết hạn,
  Redis restart, MySQL transaction rollback, RabbitMQ không khả dụng.
- Kiểm tra JWT phân quyền và token của tài khoản đã vô hiệu hóa (JWT cũ vẫn còn
  hạn cần chiến lược thu hồi/token version nếu muốn chặn ngay).

## Chưa triển khai/không được coi là đã sửa
- Đặt nhiều ghế nguyên tử trong một đơn, như đặc tả PDF (API hiện tại vẫn là 1 ghế/vé).
- Cloudflare R2 presigned media, UI người dùng/Admin, và mô hình đầy đủ
  `bookings/booking_seats/showtime_seats` trong PDF.
- Mô hình người dùng trong PDF dùng email/USER, mã nguồn hiện dùng username/CUSTOMER;
  chưa chuyển đổi vì có thể phá vỡ dữ liệu và client hiện có.
- Giá phụ thu vẫn đang gán VIP/SWEETBOX cố định thay vì bảng `seat_types.surcharge`.
- End-to-end tích hợp MySQL + Redis + RabbitMQ; không có DB thực để thử.
- Build Maven đầy đủ: Máy Windows qua RWMCP không có `java`/`mvn` khả dụng;
  môi trường container có JDK nhưng không có Maven, và không tải được gói.

**Không tuyên bố đã xử lý toàn bộ kiến trúc PDF.** Những phần chưa hoàn thiện là tồn đọng.
