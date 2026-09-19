package com.school.library.exception;

import org.springframework.http.HttpStatus;

import com.school.library.enums.ApiErrorCode;

public class InvalidIsbnException extends LibraryException {

    public InvalidIsbnException(String message) {
        super(
                ApiErrorCode.INVALID_ISBN,
                HttpStatus.BAD_REQUEST.value(),
                message
        );
    }

    public InvalidIsbnException() {
        this("ISBN checksum is invalid");
    }
}
