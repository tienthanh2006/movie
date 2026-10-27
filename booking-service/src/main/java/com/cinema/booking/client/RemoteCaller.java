package com.cinema.booking.client;

import com.cinema.common.client.RetryExecutor;
import com.cinema.common.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.function.Supplier;

/**
 * Gom cấu hình gọi service-to-service: timeout, retry và fallback.
 *  - Timeout kết nối / đọc: tránh treo thread khi service kia chậm hoặc chết.
 *  - Retry: chỉ với lỗi mạng, timeout, HTTP 5xx (các lời gọi ở đây đều là GET nên an toàn).
 *  - Fallback: hết số lần thử thì trả 503 (fail-closed). Không đặt vé nếu chưa xác minh được suất chiếu / ghế.
 */
@Component
public class RemoteCaller {
    private static final Logger log = LoggerFactory.getLogger(RemoteCaller.class);

    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final int maxAttempts;
    private final long backoffMs;

    public RemoteCaller(@Value("${services.timeout.connect-ms:2000}") int connectTimeoutMs,
                        @Value("${services.timeout.read-ms:3000}") int readTimeoutMs,
                        @Value("${services.retry.max-attempts:3}") int maxAttempts,
                        @Value("${services.retry.backoff-ms:200}") long backoffMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.maxAttempts = maxAttempts;
        this.backoffMs = backoffMs;
    }

    public RestClient newClient(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public static boolean isRetryable(RuntimeException e) {
        return e instanceof ResourceAccessException || e instanceof HttpServerErrorException;
    }

    public <T> T get(String serviceName, Supplier<T> call) {
        return RetryExecutor.execute(call, RemoteCaller::isRetryable, maxAttempts, backoffMs, last -> {
            log.warn("{} không phản hồi sau {} lần thử: {}", serviceName, maxAttempts, last.toString());
            throw new ServiceUnavailableException(
                    serviceName + " tạm thời không khả dụng, vui lòng thử lại sau", last);
        });
    }
}
