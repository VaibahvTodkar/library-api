package com.school.library.exception;


import org.springframework.http.HttpStatus;

import com.school.library.enums.ApiErrorCode;

public class PasswordResetTokenUsedException extends LibraryException {
	public PasswordResetTokenUsedException() {
		super(ApiErrorCode.PASSWORD_RESET_TOKEN_USED, HttpStatus.UNAUTHORIZED.value(),
				"Password reset token has already been used");
	}
}