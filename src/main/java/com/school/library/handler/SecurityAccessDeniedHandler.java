package com.school.library.handler;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.school.library.dto.response.ApiError;
import com.school.library.dto.response.ApiResponse;
import com.school.library.enums.ApiErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityAccessDeniedHandler
        implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityAccessDeniedHandler(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {

        ApiResponse<Void> body = ApiResponse.error(
                HttpServletResponse.SC_FORBIDDEN,
                "Access denied",
                List.of(
                        ApiError.global(
                                ApiErrorCode.FORBIDDEN.name(),
                                "Access denied"
                        )
                ),
                request.getRequestURI(),
                null
        );

        response.setStatus(
                HttpServletResponse.SC_FORBIDDEN
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
