package com.school.library.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.school.library.dto.request.UserCreateRequest;
import com.school.library.dto.request.UserUpdateRequest;
import com.school.library.dto.response.PageResponse;
import com.school.library.dto.response.UserResponse;
import com.school.library.entity.Role;
import com.school.library.entity.User;
import com.school.library.enums.UserStatus;
import com.school.library.exception.DuplicateResourceException;
import com.school.library.exception.ResourceConflictException;
import com.school.library.exception.ResourceNotFoundException;
import com.school.library.repository.RoleRepository;
import com.school.library.repository.UserRepository;
import com.school.library.util.UserSpecification;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================================================
    // CREATE
    // =========================================================

    public UserResponse create(UserCreateRequest request) {

        validateUsername(request.username());
        validateEmail(request.email());

        Role role = roleRepository
                .findById(request.roleId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Role not found: " + request.roleId()
                        )
                );

        UserStatus status =
                request.status() != null
                        ? request.status()
                        : UserStatus.ACTIVE;

        User user = User.builder()
                .username(request.username().trim())
                .passwordHash(
                        passwordEncoder.encode(
                                request.password()
                        )
                )
                .email(
                        normalizeEmail(
                                request.email()
                        )
                )
                .fullName(
                        request.fullName().trim()
                )
                .role(role)
                .status(status)
                .tokenVersion(0)
                .failedAttempts(0)
                .build();

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {

        User user = findUserById(id);

        return toResponse(user);
    }

    // =========================================================
    // LIST
    // =========================================================

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(
            String search,
            UserStatus status,
            Long roleId,
            Pageable pageable
    ) {

        Specification<User> specification =
                Specification.allOf(
                        UserSpecification.search(search),
                        UserSpecification.hasStatus(status),
                        UserSpecification.hasRoleId(roleId)
                );

        Page<User> page =
                userRepository.findAll(
                        specification,
                        pageable
                );

        Page<UserResponse> responsePage =
                page.map(this::toResponse);

        return PageResponse.from(responsePage);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public UserResponse update(
            Long id,
            UserUpdateRequest request
    ) {

        User user = findUserById(id);

        /*
         * Email update.
         */
        if (request.email() != null
                && !request.email().equalsIgnoreCase(user.getEmail())) {

            validateEmailForUpdate(
                    request.email(),
                    id
            );

            user.setEmail(
                    normalizeEmail(request.email())
            );
        }

        /*
         * Full name update.
         */
        if (request.fullName() != null
                && !request.fullName().isBlank()) {

            user.setFullName(
                    request.fullName().trim()
            );
        }

        /*
         * Role update.
         */
        if (request.roleId() != null) {

            Role role =
                    roleRepository
                            .findById(request.roleId())
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Role not found: "
                                                    + request.roleId()
                                    )
                            );

            /*
             * Avoid unnecessary token invalidation
             * when the role has not actually changed.
             */
            boolean roleChanged =
                    user.getRole() == null
                            || !user.getRole()
                                    .getId()
                                    .equals(role.getId());

            if (roleChanged) {

                user.setRole(role);

                /*
                 * Role is part of JWT authorization.
                 * Existing access tokens must become invalid
                 * if tokenVersion is checked during validation.
                 */
                incrementTokenVersion(user);
            }
        }

        return toResponse(
                userRepository.save(user)
        );
    }

    // =========================================================
    // UPDATE STATUS
    // =========================================================

    public UserResponse updateStatus(
            Long id,
            UserStatus newStatus
    ) {

        User user = findUserById(id);

        if (newStatus == null) {
            throw new ResourceConflictException(
                    "User status is required"
            );
        }

        if (user.getStatus() == newStatus) {
            return toResponse(user);
        }

        user.setStatus(newStatus);

        /*
         * Status changes can affect authentication.
         * Invalidate existing JWTs.
         */
        incrementTokenVersion(user);

        /*
         * When disabling/locking/rejecting an account,
         * existing refresh sessions should also be invalidated.
         */
        if (newStatus != UserStatus.ACTIVE) {

            user.getRefreshTokens()
                    .forEach(token ->
                            token.setRevokedAt(
                                    java.time.Instant.now()
                            )
                    );
        }

        return toResponse(
                userRepository.save(user)
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    public void delete(Long id) {

        User user = findUserById(id);

        /*
         * For a library system, hard deletion may not be
         * desirable because loans/audit records may reference
         * the user.
         *
         * Prefer DISABLED status.
         */
        user.setStatus(UserStatus.DISABLED);

        incrementTokenVersion(user);

        /*
         * Revoke all active refresh sessions.
         */
        user.getRefreshTokens()
                .forEach(token ->
                        token.setRevokedAt(
                                java.time.Instant.now()
                        )
                );

        userRepository.save(user);
    }

    // =========================================================
    // PASSWORD
    // =========================================================

    public void changePassword(
            Long id,
            String newPassword
    ) {

        User user = findUserById(id);

        user.setPasswordHash(
                passwordEncoder.encode(
                        newPassword
                )
        );

        /*
         * Invalidate all existing JWTs.
         */
        incrementTokenVersion(user);

        /*
         * Revoke all refresh sessions.
         */
        user.getRefreshTokens()
                .forEach(token ->
                        token.setRevokedAt(
                                java.time.Instant.now()
                        )
                );

        userRepository.save(user);
    }

    // =========================================================
    // FIND USER
    // =========================================================

    private User findUserById(Long id) {

        if (id == null) {
            throw new ResourceNotFoundException(
                    "User id is required"
            );
        }

        return userRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + id
                        )
                );
    }

    // =========================================================
    // TOKEN VERSION
    // =========================================================

    private void incrementTokenVersion(User user) {

        int current =
                user.getTokenVersion() == null
                        ? 0
                        : user.getTokenVersion();

        user.setTokenVersion(
                current + 1
        );
    }

    // =========================================================
    // USERNAME VALIDATION
    // =========================================================

    private void validateUsername(String username) {

        if (username == null || username.isBlank()) {
            throw new ResourceConflictException(
                    "Username is required"
            );
        }

        String normalizedUsername =
                username.trim();

        if (userRepository.existsByUsername(
                normalizedUsername
        )) {

            throw new DuplicateResourceException(
                    "Username already exists"
            );
        }
    }

    // =========================================================
    // EMAIL VALIDATION
    // =========================================================

    private void validateEmail(String email) {

        /*
         * Email is optional for UserCreateRequest.
         */
        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail =
                normalizeEmail(email);

        if (userRepository.existsByEmail(
                normalizedEmail
        )) {

            throw new DuplicateResourceException(
                    "Email already exists"
            );
        }
    }

    // =========================================================
    // EMAIL VALIDATION - UPDATE
    // =========================================================

    private void validateEmailForUpdate(
            String email,
            Long userId
    ) {

        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail =
                normalizeEmail(email);

        if (userRepository.existsByEmailAndIdNot(
                normalizedEmail,
                userId
        )) {

            throw new DuplicateResourceException(
                    "Email already exists"
            );
        }
    }

    // =========================================================
    // NORMALIZE EMAIL
    // =========================================================

    private String normalizeEmail(String email) {

        if (email == null) {
            return null;
        }

        return email
                .trim()
                .toLowerCase();
    }

    // =========================================================
    // DTO MAPPING
    // =========================================================

    private UserResponse toResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole() != null
                        ? user.getRole().getId()
                        : null,
                user.getRole() != null
                        ? user.getRole().getName()
                        : null,
                user.getStatus(),
                user.getLockedUntil(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

