package com.school.library.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(

        Instant timestamp,

        int status,

        boolean success,

        String message,

        T data,

        List<ApiError> errors,

        String path,

        String traceId

) {

    // =========================================================
    // SUCCESS
    // =========================================================

    public static <T> ApiResponse<T> success(
            int status,
            String message,
            T data,
            String path,
            String traceId
    ) {

        return new ApiResponse<>(
                Instant.now(),
                status,
                true,
                message,
                data,
                null,
                path,
                traceId
        );
    }

    // =========================================================
    // ERROR
    // =========================================================

    public static <T> ApiResponse<T> error(
            int status,
            String message,
            List<ApiError> errors,
            String path,
            String traceId
    ) {

        return new ApiResponse<>(
                Instant.now(),
                status,
                false,
                message,
                null,
                errors,
                path,
                traceId
        );
    }
}