package com.school.library.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ApiError",
        description = "Structured error information returned by the Library Management System API"
)
public record ApiError(

        @Schema(
                description = "Name of the request field that caused the error. Null for global errors.",
                example = "email",
                nullable = true
        )
        String field,

        @Schema(
                description = "Machine-readable error code",
                example = "INVALID_EMAIL",
                allowableValues = {
                        "UNAUTHENTICATED",
                        "TOKEN_EXPIRED",
                        "INVALID_REFRESH",
                        "REFRESH_REUSE",
                        "FORBIDDEN",
                        "ACCOUNT_LOCKED",
                        "ACCOUNT_DISABLED",
                        "REQUIRED",
                        "INVALID_EMAIL",
                        "INVALID_ISBN",
                        "WEAK_PASSWORD",
                        "OUT_OF_RANGE",
                        "DUPLICATE",
                        "NOT_FOUND",
                        "CONFLICT",
                        "UNPROCESSABLE",
                        "RATE_LIMITED",
                        "INTERNAL_ERROR",
                        "SERVICE_UNAVAILABLE",
                        "INVALID_PASSWORD_RESET_TOKEN",
                        "PASSWORD_RESET_TOKEN_USED"
                }
        )
        String code,

        @Schema(
                description = "Human-readable description of the error",
                example = "Invalid email address"
        )
        String message

) {

    public static ApiError of(
            String field,
            String code,
            String message
    ) {

        return new ApiError(
                field,
                code,
                message
        );
    }

    public static ApiError global(
            String code,
            String message
    ) {

        return new ApiError(
                null,
                code,
                message
        );
    }
}