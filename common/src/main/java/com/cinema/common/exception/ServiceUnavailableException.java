package com.cinema.common.exception;

/** Service phụ thuộc (movie, venue...) không phản hồi được -> HTTP 503. */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) { super(message); }
    public ServiceUnavailableException(String message, Throwable cause) { super(message, cause); }
}
