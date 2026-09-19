package com.school.library.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(
        name = "ApiResponse",
        description = """
                Standard API response envelope used by the School Library Management System.
                """
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(

        @Schema(
                description = "Timestamp when the response was generated",
                example = "2026-09-19T10:05:20.829721700Z"
        )
        Instant timestamp,

        @Schema(
                description = "HTTP status code",
                example = "200"
        )
        int status,

        @Schema(
                description = "Indicates whether the request was successfully processed",
                example = "true"
        )
        boolean success,

        @Schema(
                description = "Human-readable response message",
                example = "User retrieved successfully"
        )
        String message,

        @Schema(
                description = "Response payload. The concrete type depends on the API endpoint."
        )
        T data,

        @Schema(
                description = "List of application or validation errors."
        )
        List<ApiError> errors,

        @Schema(
                description = "HTTP request path",
                example = "/api/v1/users/7"
        )
        String path,

        @Schema(
                description = "Request correlation identifier used for log tracing",
                example = "9f7a5d8e-6d3b-4b7e-9f24-8e5f2a1c7d90"
        )
        String traceId

) {

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