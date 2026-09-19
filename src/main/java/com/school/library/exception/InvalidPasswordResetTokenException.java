package com.school.library.exception;

import org.springframework.http.HttpStatus;

import com.school.library.enums.ApiErrorCode;

public class InvalidPasswordResetTokenException extends LibraryException {

    public InvalidPasswordResetTokenException() {
        super(
                ApiErrorCode.INVALID_PASSWORD_RESET_TOKEN,
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid or expired password reset token"
        );
    }

    public InvalidPasswordResetTokenException(String message) {
        super(
                ApiErrorCode.INVALID_PASSWORD_RESET_TOKEN,
                HttpStatus.UNAUTHORIZED.value(),
                message
        );
    }
}

