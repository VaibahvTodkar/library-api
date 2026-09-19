package com.school.library.exception;

import org.springframework.http.HttpStatus;

import com.school.library.enums.ApiErrorCode;

public class TokenExpiredException extends LibraryException {

    public TokenExpiredException() {
        super(
                ApiErrorCode.TOKEN_EXPIRED,
                HttpStatus.UNAUTHORIZED.value(),
                "Access token has expired"
        );
    }

	public TokenExpiredException(String string) {
		super(
				ApiErrorCode.TOKEN_EXPIRED,
				HttpStatus.UNAUTHORIZED.value(),
				string
		);
	}
}
