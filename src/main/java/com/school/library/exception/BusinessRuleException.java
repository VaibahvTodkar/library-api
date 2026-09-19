package com.school.library.exception;

import org.springframework.http.HttpStatus;

import com.school.library.enums.ApiErrorCode;

public class BusinessRuleException extends LibraryException {

    public BusinessRuleException(String message) {
        super(
                ApiErrorCode.UNPROCESSABLE,
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                message
        );
    }
}
