-- Review/back up first. Tested only by static inspection, NOT executed against MySQL.
-- Affected physical column names assume Hibernate's Spring Boot naming conventions.
USE booking_db;
ALTER TABLE tickets
    ADD COLUMN expires_at DATETIME(6) NULL,
    ADD COLUMN paid_at DATETIME(6) NULL,
    ADD COLUMN lock_token VARCHAR(64) NULL,
    ADD COLUMN idempotency_key VARCHAR(100) NULL;
-- Entity dùng VARCHAR cho status để có thêm giá trị EXPIRED (cột ENUM cũ của MySQL không có giá trị này).
ALTER TABLE tickets MODIFY COLUMN status VARCHAR(20) NOT NULL;
ALTER TABLE tickets ADD CONSTRAINT uk_ticket_user_idem UNIQUE (user_id, idempotency_key);
CREATE TABLE seat_claims (
    claim_key VARCHAR(100) PRIMARY KEY,
    showtime_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    ticket_id BIGINT NOT NULL,
    CONSTRAINT uk_seat_claim_showtime_seat UNIQUE (showtime_id, seat_id)
) ENGINE=InnoDB;
-- Historical PAID rows keep ownership. Abort before this step if preflight reported duplicates.
INSERT INTO seat_claims (claim_key, showtime_id, seat_id, ticket_id)
SELECT CONCAT(t.showtime_id, ':', t.seat_id), t.showtime_id, t.seat_id, t.id
FROM tickets t WHERE t.status='PAID';
-- Historical pending are made EXPIRED instead of being silently accepted without a lock token.
UPDATE tickets SET status='EXPIRED' WHERE status='PENDING' AND lock_token IS NULL;

USE movie_db;
ALTER TABLE movies ADD COLUMN duration_minutes INT NULL;
UPDATE movies SET duration_minutes=120 WHERE duration_minutes IS NULL; -- Verify actual movie lengths after migration.
ALTER TABLE showtimes ADD COLUMN end_date DATE NULL, ADD COLUMN status VARCHAR(24) NULL;
UPDATE showtimes SET end_date=show_date WHERE end_date IS NULL AND end_time>=start_time;
UPDATE showtimes SET end_date=DATE_ADD(show_date, INTERVAL 1 DAY)
    WHERE end_date IS NULL AND end_time<start_time;
UPDATE showtimes SET status='OPEN' WHERE status IS NULL;
CREATE TABLE room_schedule_mutex (room_id BIGINT PRIMARY KEY) ENGINE=InnoDB;

USE venue_db;
ALTER TABLE seats ADD CONSTRAINT uk_room_seat_name UNIQUE(room_id, seat_name);
ALTER TABLE seats ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE';
