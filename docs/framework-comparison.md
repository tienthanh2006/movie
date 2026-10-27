# So sánh Spring Boot với các framework khác cho một microservice

Bảng so sánh mang tính định tính, dùng làm tài liệu cho bài thực hành. Con số cụ thể (thời gian khởi động, bộ nhớ) phụ thuộc cấu hình và phiên bản, nên cần tự đo trên máy nhóm nếu muốn đưa vào báo cáo.

| Tiêu chí | Spring Boot (Java) | Quarkus (Java) | Express (Node.js) |
|---|---|---|---|
| Ngôn ngữ | Java | Java / Kotlin | JavaScript / TypeScript |
| Hệ sinh thái | Rất rộng: Spring Data JPA, Security, AMQP, Redis, springdoc | Rộng, hỗ trợ nhiều chuẩn Jakarta/MicroProfile; ít thư viện hơn Spring | Rất nhiều gói npm; tự ghép thành phần |
| Cấu hình | Tự cấu hình bằng starter, ít phải viết tay | Tự cấu hình, thiên về build-time | Tối giản, phải tự chọn và cấu hình từng phần |
| Thời gian khởi động / bộ nhớ | Thường chậm và tốn hơn | Thiết kế để khởi động nhanh, nhẹ (đặc biệt khi build native) | Khởi động nhanh, nhẹ |
| Truy cập CSDL | JPA/Hibernate, Spring Data | Hibernate ORM with Panache | Tự chọn: Sequelize, TypeORM, Prisma... |
| Bảo mật | Spring Security, tích hợp JWT, phân quyền theo method | Quarkus Security, OIDC | Tự ghép (passport, jsonwebtoken) |
| Tài liệu API | springdoc-openapi | SmallRye OpenAPI | swagger-ui-express |
| Mô hình xử lý | Servlet (thread) hoặc WebFlux (reactive) | Hỗ trợ cả blocking và reactive | Đơn luồng, non-blocking I/O |
| Đường cong học tập | Vừa, nhiều khái niệm (bean, annotation) | Vừa, tài liệu ít hơn Spring | Thấp để bắt đầu, khó giữ cấu trúc khi dự án lớn |
| Phù hợp | Hệ thống nghiệp vụ lớn, nhóm quen Java | Dịch vụ nhỏ, cần khởi động nhanh, container/serverless | Dịch vụ I/O nhẹ, nhóm quen JavaScript |

## Kết luận cho dự án
- Nhóm dùng **Spring Boot** cho cả 5 service vì dùng chung được module `common` (JWT, xử lý lỗi, DTO sự kiện), và đã quen với Spring Data JPA, Spring Security.
- Phần ghế hiện nằm trong `venue-service` (Spring Boot). Đề bài gợi ý làm Seat Service bằng Quarkus hoặc Express để thấy sự khác biệt. Nếu muốn đúng đề, cần viết thêm một service ghế riêng bằng framework đó và giữ cùng hợp đồng REST (`GET /api/v1/seats/{id}`) để `booking-service` gọi không phải đổi gì.
