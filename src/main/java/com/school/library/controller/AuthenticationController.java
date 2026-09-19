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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(
        name = "Authentication",
        description = "User registration, authentication, token management and password recovery APIs"
)
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
    @Operation(
			summary = "Register a new user",
			description = """
					Registers a new user with the role of STUDENT and status of PENDING.
					The registration will require approval by an administrator before the user can log in.
					"""
	)
    @ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
				responseCode = "201",
				description = "Registration submitted successfully. Awaiting approval."
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
				responseCode = "400",
				description = "Invalid request data"
		)
	})
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
    @Operation(
			summary = "Authenticate user and issue tokens",
			description = """
					Authenticates a user with username and password.
					Issues an access token and a refresh token.
					The refresh token is stored in an HttpOnly + Secure cookie.
					"""
	)
    @ApiResponses({
    			@io.swagger.v3.oas.annotations.responses.ApiResponse(
				responseCode = "200",
				description = "Login successful"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
				responseCode = "401",
				description = "Invalid username or password"
		)
    })
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
    @Operation(
    		summary = "Refresh access token using refresh token",
    					description = "Refreshes the access token using the refresh token stored in an HttpOnly + Secure cookie."
    )
    @ApiResponses({
    			@io.swagger.v3.oas.annotations.responses.ApiResponse(
				responseCode = "200",
				description = "Token refreshed successfully"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
				responseCode = "401",
				description = "Invalid or expired refresh token"
		)
    })
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
    @Operation(
			summary = "Logout user and invalidate refresh token",
			description = """
					Logs out the user and invalidates the refresh token.
					Optionally, logs out from all devices if specified.
					"""
	)
    @ApiResponses({
    	@io.swagger.v3.oas.annotations.responses.ApiResponse(
    							responseCode = "204",
    							description = "Logout successful"
    							),
    	@io.swagger.v3.oas.annotations.responses.ApiResponse(
								responseCode = "204",
								description = "Logout All Devices successful"
								)
    })
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
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
    @Operation(
			summary = "Get currently authenticated user",
			description = "Returns the currently authenticated user's information."
	)
    @ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
								responseCode = "200",
								description = "Authenticated user retrieved successfully"
								),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
								responseCode = "401",
								description = "Unauthorized"
								)
	})
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
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
    @Operation(
    					summary = "Request password reset",
    					description = """
								Requests a password reset for the specified email address.
								An email with a password reset link will be sent if the email exists.
								Always returns 202 to prevent account enumeration.
								"""
    )
    @ApiResponses({
    			@io.swagger.v3.oas.annotations.responses.ApiResponse(
								responseCode = "202",
								description = "Password reset request accepted"
								)
    			
    })
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
    @Operation(
    					summary = "Reset password using token",
    					description = "Resets the user's password using a valid password reset token."
    )
    @ApiResponses({
				@io.swagger.v3.oas.annotations.responses.ApiResponse(
								responseCode = "200",
								description = "Password reset successfully"
								),
				@io.swagger.v3.oas.annotations.responses.ApiResponse(
								responseCode = "400",
								description = "Invalid or expired token"
								)
	})
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

