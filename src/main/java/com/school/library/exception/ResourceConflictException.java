package com.school.library.exception;

import org.springframework.http.HttpStatus;

import com.school.library.enums.ApiErrorCode;

public class ResourceConflictException extends LibraryException {

    public ResourceConflictException(String message) {
        super(
                ApiErrorCode.CONFLICT,
                HttpStatus.CONFLICT.value(),
                message
        );
    }
}