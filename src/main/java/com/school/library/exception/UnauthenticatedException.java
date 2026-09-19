package com.school.library.exception;

import org.springframework.http.HttpStatus;

import com.school.library.enums.ApiErrorCode;

public class UnauthenticatedException extends LibraryException {

    public UnauthenticatedException() {
        super(
                ApiErrorCode.UNAUTHENTICATED,
                HttpStatus.UNAUTHORIZED.value(),
                "Missing or invalid access token"
        );
    }

	public UnauthenticatedException(String string) {
		super(
				ApiErrorCode.UNAUTHENTICATED,
				HttpStatus.UNAUTHORIZED.value(),
				string
		);
	}
}
