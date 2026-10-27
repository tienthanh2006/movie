package com.cinema.booking.service;

import com.cinema.common.messaging.BookingEvent;
import com.cinema.common.messaging.MessagingConstants;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class BookingProducer {

    private final RabbitTemplate rabbitTemplate;

    public BookingProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendBookingEvent(BookingEvent event) {
        rabbitTemplate.convertAndSend(
            MessagingConstants.EXCHANGE_NAME,
            MessagingConstants.ROUTING_KEY,
            event
        );
        System.out.println("[BOOKING PRODUCER] Đã bắn sự kiện thành công cho vé #" + event.getTicketId());
    }
}
