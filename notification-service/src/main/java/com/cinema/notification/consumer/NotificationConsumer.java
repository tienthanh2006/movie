package com.cinema.notification.consumer;

import com.cinema.common.messaging.BookingEvent;
import com.cinema.common.messaging.MessagingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    // Lắng nghe thông điệp từ Queue
    @RabbitListener(queues = MessagingConstants.QUEUE_NAME)
    public void handleBookingNotification(BookingEvent event) {
        log.info("[NOTIFICATION] Nhận sự kiện đặt vé: vé #{} của user #{}, ghế #{}, số tiền {}",
                event.getTicketId(), event.getUserId(), event.getSeatId(), event.getFinalPrice());
        log.info("-> Đang gửi email/SMS cho khách hàng: {}", event.getMessage());
    }
}
