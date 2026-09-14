package com.school.library.dto.response;

import java.time.Instant;

import com.school.library.enums.UserStatus;

public record UserResponse(

        Long id,

        String username,

        String email,

        String fullName,

        Long roleId,

        String role,

        UserStatus status,

        Instant lockedUntil,

        Instant lastLoginAt,

        Instant createdAt,

        Instant updatedAt

) {
}