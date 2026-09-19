package com.school.library.handler;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.school.library.dto.response.ApiError;
import com.school.library.dto.response.ApiResponse;
import com.school.library.enums.ApiErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityExceptionHandler
        implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public SecurityExceptionHandler(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        ApiResponse<Void> body = ApiResponse.error(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Missing or invalid access token",
                List.of(
                        ApiError.global(
                                ApiErrorCode.UNAUTHENTICATED.name(),
                                "Missing or invalid access token"
                        )
                ),
                request.getRequestURI(),
                null
        );

        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        objectMapper.writeValue(
                response.getOutputStream(),
                body
        );
    }
}
