package com.school.library.dto.request;

import com.school.library.enums.UserStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(

        @NotBlank(message = "Username is required")
        @Size(
                min = 3,
                max = 64,
                message = "Username must be between 3 and 64 characters"
        )
        String username,

        @NotBlank(message = "Password is required")
        @Size(
                min = 8,
                max = 100,
                message = "Password must be between 8 and 100 characters"
        )
        String password,

        @Email(message = "Invalid email address")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Full name is required")
        @Size(
                max = 255,
                message = "Full name cannot exceed 255 characters"
        )
        String fullName,

        @NotNull(message = "Role is required")
        Long roleId,

        UserStatus status

) {
}
