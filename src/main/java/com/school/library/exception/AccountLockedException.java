package com.school.library.exception;

import org.springframework.http.HttpStatus;

import com.school.library.enums.ApiErrorCode;

public class AccountLockedException extends LibraryException {

    public AccountLockedException() {
        super(
                ApiErrorCode.ACCOUNT_LOCKED,
                HttpStatus.LOCKED.value(),
                "Account is locked"
        );
    }

	public AccountLockedException(String string) {
		super(
				ApiErrorCode.ACCOUNT_LOCKED,
				HttpStatus.LOCKED.value(),
				string
		);
	}
}
