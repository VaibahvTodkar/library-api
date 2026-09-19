package com.school.library.exception;
import com.school.library.enums.ApiErrorCode;

import lombok.Getter;

@Getter
public class LibraryException extends RuntimeException {

    private final ApiErrorCode code;
    private final int status;

    public LibraryException(
            ApiErrorCode code,
            int status,
            String message
    ) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public LibraryException(
            ApiErrorCode code,
            int status,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }
}
