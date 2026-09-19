package com.school.library.exception;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.school.library.dto.response.ApiError;
import com.school.library.dto.response.ApiResponse;
import com.school.library.enums.ApiErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /*
     * ============================
     * APPLICATION EXCEPTIONS
     * ============================
     */

    @ExceptionHandler(LibraryException.class)
    public ResponseEntity<ApiResponse<Void>> handleLibraryException(
            LibraryException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                ex.getStatus(),
                ex.getCode().name(),
                ex.getMessage(),
                request
        );
    }



    /*
     * ============================
     * VALIDATION
     * ============================
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        List<ApiError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        this::toApiError,
                        this::selectPreferredError,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "Request validation failed",
                errors,
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }


    private ApiError toApiError(FieldError error) {

        String code = resolveValidationCode(error);

        return ApiError.of(
                error.getField(),
                code,
                resolveValidationMessage(error)
        );
    }


    /**
     * Select the most useful validation error when multiple
     * Bean Validation constraints fail for the same field.
     *
     * Priority:
     *
     * REQUIRED
     * INVALID_EMAIL
     * OUT_OF_RANGE
     * INVALID_VALUE
     */
    private ApiError selectPreferredError(
            ApiError existing,
            ApiError candidate
    ) {

        if (validationPriority(candidate.code())
                < validationPriority(existing.code())) {

            return candidate;
        }

        return existing;
    }


    private int validationPriority(String code) {

        return switch (code) {

            case "REQUIRED" -> 1;

            case "INVALID_EMAIL" -> 2;

            case "OUT_OF_RANGE" -> 3;

            case "INVALID_VALUE" -> 4;

            default -> 5;
        };
    }


    private String resolveValidationCode(FieldError error) {

        if (error.getCode() == null) {
            return ApiErrorCode.REQUIRED.name();
        }

        return switch (error.getCode()) {

            case "NotBlank", "NotNull", "NotEmpty" ->
                    ApiErrorCode.REQUIRED.name();

            case "Email" ->
                    ApiErrorCode.INVALID_EMAIL.name();

            case "Min", "Max",
                 "DecimalMin", "DecimalMax",
                 "Size" ->
                    ApiErrorCode.OUT_OF_RANGE.name();

            default ->
                    "INVALID_VALUE";
        };
    }


    private String resolveValidationMessage(FieldError error) {

        if (error.getDefaultMessage() != null) {
            return error.getDefaultMessage();
        }

        return "Invalid value";
    }
 


    /*
     * ============================
     * DATABASE
     * ============================
     */

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {

        log.warn(
                "Database constraint violation: path={}",
                request.getRequestURI(),
                ex
        );

        String message = "Resource already exists or violates a data constraint";

        ApiResponse<Void> response = ApiResponse.error(
                HttpStatus.CONFLICT.value(),
                message,
                List.of(
                        ApiError.global(
                                ApiErrorCode.DUPLICATE.name(),
                                message
                        )
                ),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }


    /*
     * ============================
     * SPRING SECURITY
     * ============================
     */

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED.value(),
                ApiErrorCode.UNAUTHENTICATED.name(),
                "Invalid username or password",
                request
        );
    }


    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledAccount(
            DisabledException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.FORBIDDEN.value(),
                ApiErrorCode.ACCOUNT_DISABLED.name(),
                "Account is disabled",
                request
        );
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED.value(),
                ApiErrorCode.UNAUTHENTICATED.name(),
                "Authentication failed",
                request
        );
    }


    /*
     * ============================
     * ILLEGAL ARGUMENT
     * ============================
     */

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_VALUE",
                ex.getMessage(),
                request
        );
    }


    /*
     * ============================
     * UNEXPECTED ERROR
     * ============================
     */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {

        log.error(
                "Unhandled exception: method={}, path={}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ApiErrorCode.INTERNAL_ERROR.name(),
                "An unexpected error occurred",
                request
        );
    }
    
    
    
    
    
    
    
    
    
    
    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthenticated(
            UnauthenticatedException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.UNAUTHORIZED,
                ex.getCode(),
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiResponse<Void>> handleTokenExpired(
            TokenExpiredException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.UNAUTHORIZED,
                ex.getCode(),
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.UNAUTHORIZED,
                ex.getCode(),
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(RefreshTokenReuseException.class)
    public ResponseEntity<ApiResponse<Void>> handleRefreshTokenReuse(
            RefreshTokenReuseException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.UNAUTHORIZED,
                ex.getCode(),
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountLocked(
            AccountLockedException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.LOCKED,
                ex.getCode(),
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountDisabled(
            AccountDisabledException ex,
            HttpServletRequest request) {

        return buildError(
                HttpStatus.FORBIDDEN,
                ex.getCode(),
                ex.getMessage(),
                request
        );
    }
    
    
    


    /*
     * ============================
     * RESPONSE BUILDER
     * ============================
     */

    private ResponseEntity<ApiResponse<Void>> buildResponse(
            int status,
            String code,
            String message,
            HttpServletRequest request
    ) {

        ApiResponse<Void> response = ApiResponse.error(
                status,
                message,
                List.of(
                        ApiError.global(
                                code,
                                message
                        )
                ),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
    
    
    private ResponseEntity<ApiResponse<Void>> buildError(
            HttpStatus status,
            ApiErrorCode code,
            String message,
            HttpServletRequest request) {

        ApiError error = ApiError.global(
                code.name(),
                message
        );

        ApiResponse<Void> response = ApiResponse.error(
                status.value(),
                message,
                List.of(error),
                request.getRequestURI(),
                null
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}
