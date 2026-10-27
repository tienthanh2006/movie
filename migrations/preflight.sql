-- Run on the respective MySQL databases before schema changes.
USE venue_db;
SELECT room_id, seat_name, COUNT(*) AS duplicates FROM seats GROUP BY room_id, seat_name HAVING COUNT(*)>1;
USE booking_db;
SELECT showtime_id, seat_id, COUNT(*) AS paid_duplicates FROM tickets WHERE status='PAID'
GROUP BY showtime_id, seat_id HAVING COUNT(*)>1;
SELECT COUNT(*) AS missing_creation_times FROM tickets WHERE created_at IS NULL;
