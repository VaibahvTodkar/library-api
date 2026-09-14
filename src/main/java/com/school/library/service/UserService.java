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
import com.school.library.repository.RoleRepository;
import com.school.library.repository.UserRepository;
import com.school.library.security.jwt.JwtProperties;
import com.school.library.util.UserSpecification;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class UserService {

	private final UserRepository userRepository;

	private final RoleRepository roleRepository;

	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
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

		Role role = roleRepository.findById(request.roleId())
				.orElseThrow(() -> new EntityNotFoundException("Role not found: " + request.roleId()));

		UserStatus status = request.status() != null ? request.status() : UserStatus.ACTIVE;

		User user = User.builder().username(request.username().trim())
				.passwordHash(passwordEncoder.encode(request.password())).email(normalizeEmail(request.email()))
				.fullName(request.fullName().trim()).role(role).status(status).tokenVersion(0).failedAttempts(0)
				.build();

		User savedUser = userRepository.save(user);

		return toResponse(savedUser);
	}

	// =========================================================
	// GET BY ID
	// =========================================================

	@Transactional(readOnly = true)
	public UserResponse findById(Long id) {

		User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

		return toResponse(user);
	}

	// =========================================================
	// LIST
	// =========================================================

	@Transactional(readOnly = true)
	public PageResponse<UserResponse> findAll(String search, UserStatus status, Long roleId, Pageable pageable) {

		Specification<User> specification = Specification.allOf(UserSpecification.search(search),
				UserSpecification.hasStatus(status), UserSpecification.hasRoleId(roleId));

		Page<User> page = userRepository.findAll(specification, pageable);

		Page<UserResponse> responsePage = page.map(this::toResponse);

		return PageResponse.from(responsePage);
	}

	// =========================================================
	// UPDATE
	// =========================================================

	public UserResponse update(Long id, UserUpdateRequest request) {

		User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

		if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())) {

			validateEmail(request.email());

			user.setEmail(normalizeEmail(request.email()));
		}

		if (request.fullName() != null && !request.fullName().isBlank()) {

			user.setFullName(request.fullName().trim());
		}

		if (request.roleId() != null) {

			Role role = roleRepository.findById(request.roleId())
					.orElseThrow(() -> new EntityNotFoundException("Role not found: " + request.roleId()));

			user.setRole(role);

			/*
			 * Role change should invalidate existing JWTs.
			 */
			incrementTokenVersion(user);
		}

		if (request.status() != null && request.status() != user.getStatus()) {

			user.setStatus(request.status());

			/*
			 * Status change should invalidate existing JWTs.
			 */
			incrementTokenVersion(user);
		}

		return toResponse(userRepository.save(user));
	}

	// =========================================================
	// DELETE
	// =========================================================

	public void delete(Long id) {

		User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

		/*
		 * For a library system, hard deletion may not be desirable because loans/audit
		 * records may reference the user.
		 *
		 * Prefer DISABLED status in production.
		 */

		user.setStatus(UserStatus.DISABLED);

		incrementTokenVersion(user);

		userRepository.save(user);
	}

	// =========================================================
	// PASSWORD
	// =========================================================

	public void changePassword(Long id, String newPassword) {

		User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

		user.setPasswordHash(passwordEncoder.encode(newPassword));

		/*
		 * Invalidate all existing JWTs.
		 */
		incrementTokenVersion(user);

		userRepository.save(user);
	}

	// =========================================================
	// TOKEN VERSION
	// =========================================================

	private void incrementTokenVersion(User user) {

		int current = user.getTokenVersion() == null ? 0 : user.getTokenVersion();

		user.setTokenVersion(current + 1);
	}

	// =========================================================
	// USERNAME VALIDATION
	// =========================================================

	private void validateUsername(String username) {

		if (userRepository.existsByUsername(username.trim())) {

			throw new IllegalArgumentException("Username already exists");
		}
	}

	// =========================================================
	// EMAIL VALIDATION
	// =========================================================

	private void validateEmail(String email) {

		if (email == null || email.isBlank()) {
			return;
		}

		String normalized = normalizeEmail(email);

		if (userRepository.existsByEmail(normalized)) {

			throw new IllegalArgumentException("Email already exists");
		}
	}

	private String normalizeEmail(String email) {

		if (email == null) {
			return null;
		}

		return email.trim().toLowerCase();
	}

	// =========================================================
	// DTO MAPPING
	// =========================================================

	private UserResponse toResponse(User user) {

		return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(),
				user.getRole() != null ? user.getRole().getId() : null,
				user.getRole() != null ? user.getRole().getName() : null, user.getStatus(), user.getLockedUntil(),
				user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt());
	}
}
