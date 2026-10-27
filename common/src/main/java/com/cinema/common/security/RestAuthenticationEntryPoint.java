package com.cinema.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Chưa đăng nhập / token sai / token hết hạn -> 401 với JSON chuẩn (thay vì 403 mặc định). */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        SecurityErrorWriter.write(response, 401, "Unauthorized",
                "Chưa đăng nhập hoặc token không hợp lệ / đã hết hạn", request.getRequestURI());
    }
}
