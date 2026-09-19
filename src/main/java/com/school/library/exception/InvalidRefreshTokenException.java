package com.school.library.exception;

import org.springframework.http.HttpStatus;

import com.school.library.enums.ApiErrorCode;

public class InvalidRefreshTokenException extends LibraryException {

    public InvalidRefreshTokenException() {
        super(
                ApiErrorCode.INVALID_REFRESH,
                HttpStatus.UNAUTHORIZED.value(),
                "Refresh token is invalid or revoked"
        );
    }

	public InvalidRefreshTokenException(String string) {
		super(
				ApiErrorCode.INVALID_REFRESH,
				HttpStatus.UNAUTHORIZED.value(),
				string
		);
	}
}
