package com.school.library.enums;


public enum ApiErrorCode {

    // =========================
    // AUTHENTICATION
    // =========================

    UNAUTHENTICATED,
    TOKEN_EXPIRED,
    INVALID_REFRESH,
    REFRESH_REUSE,
    FORBIDDEN,
    ACCOUNT_LOCKED,
    ACCOUNT_DISABLED,

    // =========================
    // VALIDATION
    // =========================

    REQUIRED,
    INVALID_EMAIL,
    INVALID_ISBN,
    WEAK_PASSWORD,
    OUT_OF_RANGE,

    // =========================
    // DATA
    // =========================

    DUPLICATE,
    NOT_FOUND,
    CONFLICT,

    // =========================
    // BUSINESS
    // =========================

    UNPROCESSABLE,

    // =========================
    // THROTTLING
    // =========================

    RATE_LIMITED,

    // =========================
    // SERVER
    // =========================

    INTERNAL_ERROR,
    SERVICE_UNAVAILABLE, 
    
    // =========================
    // Password Reset
    // =========================

    INVALID_PASSWORD_RESET_TOKEN,
    PASSWORD_RESET_TOKEN_USED
    
}
