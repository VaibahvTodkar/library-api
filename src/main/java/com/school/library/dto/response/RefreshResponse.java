package com.school.library.dto.response;

public record RefreshResponse(

		String accessToken,

		String refreshToken,

		String tokenType,

		long expiresIn) {
}
