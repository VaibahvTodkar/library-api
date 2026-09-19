package com.school.library.exception;

import org.springframework.http.HttpStatus;

import com.school.library.enums.ApiErrorCode;

public class WeakPasswordException extends LibraryException {

    public WeakPasswordException(String message) {
        super(
                ApiErrorCode.WEAK_PASSWORD,
                HttpStatus.BAD_REQUEST.value(),
                message
        );
    }

    public WeakPasswordException() {
        this("Password does not meet security requirements");
    }
}
