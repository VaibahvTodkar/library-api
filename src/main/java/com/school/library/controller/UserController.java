package com.school.library.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.school.library.dto.request.UserCreateRequest;
import com.school.library.dto.request.UserUpdateRequest;
import com.school.library.dto.response.ApiResponse;
import com.school.library.dto.response.PageResponse;
import com.school.library.dto.response.UserResponse;
import com.school.library.enums.UserStatus;
import com.school.library.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(
            UserService userService
    ) {
        this.userService = userService;
    }

    // =========================================================
    // CREATE
    // =========================================================

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody UserCreateRequest request,
            HttpServletRequest httpRequest
    ) {

        UserResponse data =
                userService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                HttpStatus.CREATED.value(),
                                "User created successfully",
                                data,
                                httpRequest.getRequestURI(),
                                getTraceId(httpRequest)
                        )
                );
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> findById(
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {

        UserResponse data =
                userService.findById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "User retrieved successfully",
                        data,
                        httpRequest.getRequestURI(),
                        getTraceId(httpRequest)
                )
        );
    }

    // =========================================================
    // LIST
    // =========================================================

    @GetMapping
    public ResponseEntity<
            ApiResponse<PageResponse<UserResponse>>
            > findAll(

            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            UserStatus status,

            @RequestParam(required = false)
            Long roleId,

            Pageable pageable,

            HttpServletRequest httpRequest
    ) {

        PageResponse<UserResponse> data =
                userService.findAll(
                        search,
                        status,
                        roleId,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Users retrieved successfully",
                        data,
                        httpRequest.getRequestURI(),
                        getTraceId(httpRequest)
                )
        );
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id,

            @Valid
            @RequestBody
            UserUpdateRequest request,

            HttpServletRequest httpRequest
    ) {

        UserResponse data =
                userService.update(
                        id,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "User updated successfully",
                        data,
                        httpRequest.getRequestURI(),
                        getTraceId(httpRequest)
                )
        );
    }

    // =========================================================
    // DELETE / DISABLE
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {

        userService.delete(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "User disabled successfully",
                        null,
                        httpRequest.getRequestURI(),
                        getTraceId(httpRequest)
                )
        );
    }

    // =========================================================
    // TRACE ID
    // =========================================================

    private String getTraceId(
            HttpServletRequest request
    ) {

        String traceId =
                request.getHeader("X-Trace-Id");

        return traceId != null
                ? traceId
                : java.util.UUID.randomUUID().toString();
    }
}
