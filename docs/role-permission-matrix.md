# Ma trận phân quyền (Role–Permission Matrix)

Ký hiệu: ✔ được phép, ✘ bị từ chối (403), 🔓 công khai (không cần token), 🔒 chỉ chủ tài nguyên (người khác 403).

## Vai trò đã cài đặt
| Vai trò | Mô tả |
|---|---|
| Khách (chưa đăng nhập) | Chỉ xem dữ liệu công khai, đăng ký, đăng nhập |
| CUSTOMER | Khách đã đăng nhập: đặt, thanh toán, hủy, xem vé của mình |
| ADMIN | Quản trị: quản lý phim, suất chiếu, rạp, phòng, ghế, vé |

## user-service (8081)
| API | Khách | CUSTOMER | ADMIN |
|---|---|---|---|
| POST /api/v1/auth/register | 🔓 | 🔓 | 🔓 |
| POST /api/v1/auth/login | 🔓 | 🔓 | 🔓 |

## movie-service (8082)
| API | Khách | CUSTOMER | ADMIN |
|---|---|---|---|
| GET /api/v1/movies, /movies/{id}, /movies/now-showing, /movies/coming-soon | 🔓 | ✔ | ✔ |
| GET /api/v1/showtimes (theo phim và ngày), /showtimes/{id} | 🔓 | ✔ | ✔ |
| POST, PUT, DELETE /api/v1/movies | ✘ (401) | ✘ (403) | ✔ |
| POST, PUT, DELETE /api/v1/showtimes | ✘ (401) | ✘ (403) | ✔ |

## venue-service (8083)
| API | Khách | CUSTOMER | ADMIN |
|---|---|---|---|
| GET /api/v1/cinemas, /rooms/{id}/seats, /seats/{id}, /seats/room/{roomId} | 🔓 | ✔ | ✔ |
| POST, DELETE /api/v1/cinemas | ✘ (401) | ✘ (403) | ✔ |
| POST, PUT, DELETE /api/v1/rooms; POST /rooms/{id}/seats/generate | ✘ (401) | ✘ (403) | ✔ |
| POST, PUT, **PATCH**, DELETE /api/v1/seats | ✘ (401) | ✘ (403) | ✔ |

## booking-service (8084)
| API | Khách | CUSTOMER | ADMIN |
|---|---|---|---|
| POST /api/v1/tickets (đặt vé, hỗ trợ Idempotency-Key) | ✘ (401) | ✔ | ✔ |
| GET /api/v1/tickets/my-tickets | ✘ (401) | ✔ | ✔ |
| GET /api/v1/users/{userId}/tickets | ✘ (401) | 🔒 | ✔ |
| GET /api/v1/tickets/{id} | ✘ (401) | 🔒 | ✔ |
| POST /api/v1/tickets/{id}/pay | ✘ (401) | 🔒 | 🔒 (chỉ vé của chính mình) |
| POST /api/v1/tickets/{id}/cancel | ✘ (401) | 🔒 | ✔ |
| GET /api/v1/tickets (toàn bộ vé) | ✘ (401) | ✘ (403) | ✔ |
| PATCH /api/v1/tickets/{id}/status (chỉ chuyển sang CANCELLED) | ✘ (401) | ✘ (403) | ✔ |
| DELETE /api/v1/tickets/{id} (chỉ vé đã hủy/hết hạn) | ✘ (401) | ✘ (403) | ✔ |

## Quy tắc đặc biệt
- Đặt `PAID` bằng PATCH bị từ chối (409) kể cả ADMIN. Thanh toán chỉ qua `/pay`.
- Vé của người khác: 403, không tiết lộ thông tin vé.
- Vai trò `STAFF` chưa cài đặt trong mã nguồn hiện tại.
