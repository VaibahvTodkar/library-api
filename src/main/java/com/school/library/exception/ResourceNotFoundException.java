package com.school.library.exception;

import org.springframework.http.HttpStatus;

import com.school.library.enums.ApiErrorCode;

public class ResourceNotFoundException extends LibraryException {

    public ResourceNotFoundException(String message) {
        super(
                ApiErrorCode.NOT_FOUND,
                HttpStatus.NOT_FOUND.value(),
                message
        );
    }
}	
