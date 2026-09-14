package com.school.library.dto.request;

import com.school.library.enums.UserStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        @Email(message = "Invalid email address")
        @Size(max = 255)
        String email,

        @Size(
                max = 255,
                message = "Full name cannot exceed 255 characters"
        )
        String fullName,

        Long roleId,

        UserStatus status

) {
}