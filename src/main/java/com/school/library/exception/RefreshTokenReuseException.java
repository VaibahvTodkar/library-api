package com.school.library.exception;

import org.springframework.http.HttpStatus;

import com.school.library.enums.ApiErrorCode;

public class RefreshTokenReuseException extends LibraryException {

    public RefreshTokenReuseException() {
        super(
                ApiErrorCode.REFRESH_REUSE,
                HttpStatus.UNAUTHORIZED.value(),
                "Refresh token has already been used"
        );
    }

	public RefreshTokenReuseException(String string) {

		super(
				ApiErrorCode.REFRESH_REUSE,
				HttpStatus.UNAUTHORIZED.value(),
				string
		);
	}
}
