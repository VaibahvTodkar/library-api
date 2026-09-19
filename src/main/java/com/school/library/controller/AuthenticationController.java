package com.school.library.controller;

import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.school.library.dto.request.ForgotPasswordRequest;
import com.school.library.dto.request.LoginRequest;
import com.school.library.dto.request.LogoutRequest;
import com.school.library.dto.request.RegisterUserRequest;
import com.school.library.dto.request.ResetPasswordRequest;
import com.school.library.dto.response.ApiResponse;
import com.school.library.dto.response.AuthUserResponse;
import com.school.library.dto.response.LoginResponse;
import com.school.library.dto.response.RefreshResponse;
import com.school.library.dto.response.RegisterUserResponse;
import com.school.library.service.AuthenticationService;
import com.school.library.service.PasswordResetService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;

    public AuthenticationController(
            AuthenticationService authenticationService,
            PasswordResetService passwordResetService
    ) {
        this.authenticationService = authenticationService;
        this.passwordResetService = passwordResetService;
    }

    /**
     * POST /api/v1/auth/register
     *
     * Public endpoint.
     *
     * User is created as:
     * - Role   : STUDENT
     * - Status : PENDING
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegisterUserResponse> register(
            @Valid @RequestBody RegisterUserRequest request,
            HttpServletRequest httpRequest
    ) {

        RegisterUserResponse response =
                authenticationService.registerUser(request);

        return ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Registration submitted successfully. Awaiting approval.",
                response,
                httpRequest.getRequestURI(),
                null
        );
    }

    /**
     * POST /api/v1/auth/login
     *
     * Public endpoint.
     *
     * Refresh token is stored in
     * HttpOnly + Secure cookie.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response,
            HttpServletRequest httpRequest
    ) {

        LoginResponse result =
                authenticationService.login(
                        request.username(),
                        request.password(),
                        request.deviceId()
                );

        /*
         * Store refresh token in HttpOnly + Secure cookie.
         */
        ResponseCookie refreshCookie =
                ResponseCookie
                        .from(
                                REFRESH_COOKIE,
                                result.refreshToken()
                        )
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Strict")
                        .path("/api/v1/auth")
                        .maxAge(Duration.ofDays(7))
                        .build();

        response.addHeader(
                "Set-Cookie",
                refreshCookie.toString()
        );

        /*
         * Do not expose refresh token in JSON.
         */
        LoginResponse responseBody =
                new LoginResponse(
                        result.accessToken(),
                        result.tokenType(),
                        result.expiresIn(),
                        null
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        ApiResponse.success(
                                HttpStatus.OK.value(),
                                "Login successful",
                                responseBody,
                                httpRequest.getRequestURI(),
                                null
                        )
                );
    }

    /**
     * POST /api/v1/auth/refresh
     *
     * Refresh token is read from HttpOnly cookie.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @CookieValue(
                    name = REFRESH_COOKIE,
                    required = false
            )
            String refreshToken,
            HttpServletResponse response,
            HttpServletRequest request
    ) {

        RefreshResponse result =
                authenticationService.refresh(
                        refreshToken
                );

        /*
         * Rotate refresh token and replace cookie.
         */
        ResponseCookie refreshCookie =
                ResponseCookie
                        .from(
                                REFRESH_COOKIE,
                                result.refreshToken()
                        )
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Strict")
                        .path("/api/v1/auth")
                        .maxAge(Duration.ofDays(7))
                        .build();

        response.addHeader(
                "Set-Cookie",
                refreshCookie.toString()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Token refreshed successfully",
                        result,
                        request.getRequestURI(),
                        null
                )
        );
    }

    /**
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody LogoutRequest request,
            @CookieValue(
                    name = REFRESH_COOKIE,
                    required = false
            )
            String refreshToken,
            Authentication authentication,
            HttpServletResponse response
    ) {

        Long userId =
                ((Number) authentication.getPrincipal())
                        .longValue();

        if (request.allDevices()) {

            authenticationService.logoutAllDevices(
                    userId
            );

        } else {

            authenticationService.logout(
                    userId,
                    refreshToken
            );
        }

        /*
         * Delete refresh-token cookie.
         */
        ResponseCookie deleteCookie =
                ResponseCookie
                        .from(
                                REFRESH_COOKIE,
                                ""
                        )
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Strict")
                        .path("/api/v1/auth")
                        .maxAge(Duration.ZERO)
                        .build();

        response.addHeader(
                "Set-Cookie",
                deleteCookie.toString()
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    /**
     * GET /api/v1/auth/me
     *
     * Returns currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthUserResponse>> me(
            Authentication authentication,
            HttpServletRequest request
    ) {

        Long userId =
                ((Number) authentication.getPrincipal())
                        .longValue();

        AuthUserResponse user =
                authenticationService.me(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Authenticated user retrieved successfully",
                        user,
                        request.getRequestURI(),
                        null
                )
        );
    }

    /**
     * POST /api/v1/auth/forgot-password
     *
     * Always returns 202 to prevent account enumeration.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {

        passwordResetService.requestPasswordReset(
                request.email()
        );

        /*
         * Always return 202.
         *
         * This prevents attackers from determining
         * whether an email address exists.
         */
        return ResponseEntity
                .accepted()
                .build();
    }

    /**
     * POST /api/v1/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {

        passwordResetService.resetPassword(
                request.token(),
                request.newPassword()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Password reset successfully",
                        null,
                        httpRequest.getRequestURI(),
                        null
                )
        );
    }
}

