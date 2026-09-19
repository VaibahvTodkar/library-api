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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
@Tag(
        name = "Users",
        description = "User management and account administration APIs"
)
@SecurityRequirement(name = "bearerAuth")
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
    @Operation(summary = "Create a new user", description = "Creates a new user account with the provided details.")	
    @ApiResponses(value = {	
    		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User created successfully",	
					content = @Content(mediaType = "application/json",	
							schema = @Schema(implementation = ApiResponse.class))),	
    		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data",	
					content = @Content(mediaType = "application/json",	
							schema = @Schema(implementation = ApiResponse.class))),	
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username or email already exists",	
					content = @Content(mediaType = "application/json",	
							schema = @Schema(implementation = ApiResponse.class)))	
	})	
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
    @Operation(summary = "Get user by ID", description = "Retrieves a user account by its unique ID.")
    @ApiResponses(value = {
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User retrieved successfully",
					content = @Content(mediaType = "application/json",
							schema = @Schema(implementation = ApiResponse.class))),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found",
					content = @Content(mediaType = "application/json",
							schema = @Schema(implementation = ApiResponse.class)))
	})	
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
    @Operation(summary = "List users", description = "Retrieves a paginated list of users with optional filtering by search term, status, and role ID.")
    @ApiResponses({
    			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully",
				content = @Content(mediaType = "application/json",
						schema = @Schema(implementation = ApiResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request parameters",
				content = @Content(mediaType = "application/json",
						schema = @Schema(implementation = ApiResponse.class)))
    })
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
    @Operation(summary = "Update user", description = "Updates an existing user account with the provided details.")
    @ApiResponses({
    			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User updated successfully",
				content = @Content(mediaType = "application/json",
						schema = @Schema(implementation = ApiResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input data",
				content = @Content(mediaType = "application/json",
						schema = @Schema(implementation = ApiResponse.class))),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found",
				content = @Content(mediaType = "application/json",
						schema = @Schema(implementation = ApiResponse.class)))
	})
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
    @Operation(summary = "Disable user", description = "Disables a user account by its unique ID. The user will no longer be able to log in.")
    @ApiResponses({
    			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User disabled successfully",
    									content = @Content(mediaType = "application/json",
    									schema = @Schema(implementation = ApiResponse.class))),
    					@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found",
    									content = @Content(mediaType = "application/json",
										schema = @Schema(implementation = ApiResponse.class)))
    			
    })
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
    @Operation(summary = "Trace By ID" , hidden = true, description = "Retrieves the trace ID from the request headers or generates a new one if not present.")
    @ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Trace ID retrieved successfully",
					content = @Content(mediaType = "application/json",
							schema = @Schema(implementation = ApiResponse.class)))
	})
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
