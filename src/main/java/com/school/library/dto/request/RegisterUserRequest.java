package com.school.library.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(

        @Schema(
                description = "Unique username",
                example = "john.doe",
                minLength = 3,
                maxLength = 64
        )
        @NotBlank(message = "Username is required")
        @Size(
                min = 3,
                max = 64,
                message = "Username must be between 3 and 64 characters"
        )
        String username,

        @Schema(
                description = "User password",
                example = "StrongPassword@123",
                minLength = 8,
                maxLength = 100,
                format = "password"
        )
        @NotBlank(message = "Password is required")
        @Size(
                min = 8,
                max = 100,
                message = "Password must be between 8 and 100 characters"
        )
        String password,

        @Schema(
                description = "User email address",
                example = "john.doe@school.com"
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address")
        @Size(max = 255)
        String email,

        @Schema(
                description = "Full name of the student",
                example = "John Doe",
                maxLength = 255
        )
        @NotBlank(message = "Full name is required")
        @Size(max = 255)
        String fullName
) {
}