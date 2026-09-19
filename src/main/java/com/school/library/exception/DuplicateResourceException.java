package com.school.library.exception;

import org.springframework.http.HttpStatus;

import com.school.library.enums.ApiErrorCode;

public class DuplicateResourceException extends LibraryException {

    public DuplicateResourceException(String message) {
        super(
                ApiErrorCode.DUPLICATE,
                HttpStatus.CONFLICT.value(),
                message
        );
    }
}