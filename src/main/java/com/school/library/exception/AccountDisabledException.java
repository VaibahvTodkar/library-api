package com.school.library.exception;

import org.springframework.http.HttpStatus;

import com.school.library.enums.ApiErrorCode;

public class AccountDisabledException extends LibraryException {

    public AccountDisabledException() {
        super(
                ApiErrorCode.ACCOUNT_DISABLED,
                HttpStatus.FORBIDDEN.value(),
                "Account is disabled"
        );
    }

	public AccountDisabledException(String string) {
		super(
				ApiErrorCode.ACCOUNT_DISABLED,
				HttpStatus.FORBIDDEN.value(),
				string
		);
	}
}
