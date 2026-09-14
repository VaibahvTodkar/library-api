package com.school.library.dto.response;

public record RegisterUserResponse(
		Long id,
        String username,
        String email,
        String fullName,
        String role,
        String status
) {}
