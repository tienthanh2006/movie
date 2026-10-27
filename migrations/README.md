# Quy trình cập nhật schema MySQL (không tự chạy trên dữ liệu thật)

1. Sao lưu `user_db`, `movie_db`, `venue_db`, `booking_db` và thử trên bản phục hồi.
2. Kiểm tra các truy vấn `preflight.sql`, giải quyết mọi dòng trùng trước khi bật unique constraints.
3. Tắt ứng dụng ghi dữ liệu, thực hiện `schema_update_mysql8.sql` **một lần** theo thứ tự.
4. Đảm bảo `JWT_SECRET`, mật khẩu DB/RabbitMQ đã cấu hình an toàn, rồi chạy `mvn clean verify`.
5. Chỉ khi schema đã khớp, khởi động với `DDL_AUTO=validate` (mặc định).

*Rủi ro:* Đây là thay đổi schema đáng kể, không được áp dụng trực tiếp khi chưa có backup,
kiểm thử bản staging và cửa sổ bảo trì. Scripts chưa chạy trên cơ sở dữ liệu thực tế.
Các vé PENDING đời cũ không có lock token sẽ không được phép thanh toán, cần khách đặt lại;
không nên tự phát hành vé cho các bản ghi thiếu bằng chứng thanh toán.
