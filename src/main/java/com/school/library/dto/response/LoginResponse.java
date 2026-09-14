package com.school.library.dto.response;

public record LoginResponse(

        String accessToken,

        String tokenType,

        long expiresIn,

        String refreshToken
) {
}
