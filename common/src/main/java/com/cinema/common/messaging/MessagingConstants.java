package com.cinema.common.messaging;

public final class MessagingConstants {
    public static final String QUEUE_NAME = "bookingQueue";
    public static final String EXCHANGE_NAME = "bookingExchange";
    public static final String ROUTING_KEY = "booking.routingKey";

    private MessagingConstants() {}
}
