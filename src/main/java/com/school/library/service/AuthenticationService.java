package com.school.library.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.school.library.dto.request.RegisterUserRequest;
import com.school.library.dto.response.AuthUserResponse;
import com.school.library.dto.response.LoginResponse;
import com.school.library.dto.response.RefreshResponse;
import com.school.library.dto.response.RegisterUserResponse;
import com.school.library.entity.Permission;
import com.school.library.entity.RefreshToken;
import com.school.library.entity.Role;
import com.school.library.entity.User;
import com.school.library.enums.UserStatus;
import com.school.library.exception.AccountDisabledException;
import com.school.library.exception.AccountLockedException;
import com.school.library.exception.DuplicateResourceException;
import com.school.library.exception.InvalidRefreshTokenException;
import com.school.library.exception.RefreshTokenReuseException;
import com.school.library.exception.ResourceConflictException;
import com.school.library.exception.ResourceNotFoundException;
import com.school.library.exception.TokenExpiredException;
import com.school.library.exception.UnauthenticatedException;
import com.school.library.repository.RefreshTokenRepository;
import com.school.library.repository.RoleRepository;
import com.school.library.repository.UserRepository;
import com.school.library.security.jwt.JwtService;

@Service
@Transactional
public class AuthenticationService {
	private static final int REFRESH_TOKEN_BYTES = 64;
	private static final long ACCESS_TOKEN_EXPIRES_IN_SECONDS = 900;
	private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;
	private static final String STUDENT_ROLE = "STUDENT";
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthenticationService(UserRepository userRepository, RoleRepository roleRepository,
			RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	/** * REGISTER * * Public registration. * * Role = STUDENT * Status = PENDING */
	public RegisterUserResponse registerUser(RegisterUserRequest request) {
		/* * Username uniqueness. */ if (userRepository.existsByUsername(request.username())) {
			throw new DuplicateResourceException("Username already exists");
		}
		/* * Email uniqueness. */ if (userRepository.existsByEmail(request.email())) {
			throw new DuplicateResourceException("Email already exists");
		}
		/*
		 * * Self-registration is ALWAYS STUDENT. * * The client cannot select the role.
		 */ Role studentRole = roleRepository.findByName(STUDENT_ROLE)
				.orElseThrow(() -> new ResourceConflictException("STUDENT role is not configured"));
		User user = User.builder().username(request.username()).passwordHash(passwordEncoder.encode(request.password()))
				.email(request.email()).fullName(request.fullName()).role(studentRole)
				/* * New users must be approved * before login. */ .status(UserStatus.PENDING).tokenVersion(0)
				.failedAttempts(0).build();
		user = userRepository.save(user);
		return new RegisterUserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(),
				user.getRole().getName(), user.getStatus().name());
	}

	/** * LOGIN */
	public LoginResponse login(String username, String password, String deviceId) {
		/* * Do not reveal whether the username exists. */ User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UnauthenticatedException("Invalid username or password"));
		/* * Handle account status explicitly. */ validateAccountStatus(user);
		/* * Password verification. */ if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			/*
			 * * Invalid password. * * Failed-attempt / account-locking logic * can be added
			 * here.
			 */ throw new UnauthenticatedException("Invalid username or password");
		}
		String role = user.getRole().getName();
		List<String> permissions = user.getRole().getPermissions().stream().map(Permission::getCode).sorted().toList();
		/* * Generate access token. */ String accessToken = jwtService.generateAccessToken(user.getId(),
				user.getUsername(), user.getFullName(), user.getEmail(), role, permissions, user.getTokenVersion());
		/* * Generate opaque refresh token. */ String rawRefreshToken = generateRefreshToken();
		Instant now = Instant.now();
		RefreshToken refreshToken = RefreshToken.builder().user(user).tokenHash(hashToken(rawRefreshToken))
				.deviceId(deviceId).issuedAt(now).expiresAt(now.plus(REFRESH_TOKEN_EXPIRY_DAYS, ChronoUnit.DAYS))
				.build();
		refreshTokenRepository.save(refreshToken);
		user.setLastLoginAt(now);
		userRepository.save(user);
		return new LoginResponse(accessToken, "Bearer", ACCESS_TOKEN_EXPIRES_IN_SECONDS, rawRefreshToken);
	}

	/** * REFRESH TOKEN * * Refresh-token rotation is used. */
	public RefreshResponse refresh(String rawRefreshToken) {
		/* * Missing refresh token. */ if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new InvalidRefreshTokenException("Refresh token is required");
		}
		String tokenHash = hashToken(rawRefreshToken);
		/* * Token must exist. */ RefreshToken currentToken = refreshTokenRepository.findByTokenHash(tokenHash)
				.orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));
		Instant now = Instant.now();
		/*
		 * * A revoked refresh token must never * be accepted again. * * This also
		 * detects refresh-token reuse.
		 */ if (currentToken.getRevokedAt() != null) {
			throw new RefreshTokenReuseException("Refresh token has already been used or revoked");
		}
		/* * Check refresh-token expiration. */ if (currentToken.getExpiresAt().isBefore(now)) {
			throw new TokenExpiredException("Refresh token has expired");
		}
		User user = currentToken.getUser();
		/* * Account must still be active. */ validateAccountStatus(user);
		String role = user.getRole().getName();
		List<String> permissions = user.getRole().getPermissions().stream().map(Permission::getCode).sorted().toList();
		/* * Generate new access token. */ String accessToken = jwtService.generateAccessToken(user.getId(),
				user.getUsername(), user.getFullName(), user.getEmail(), role, permissions, user.getTokenVersion());
		/* * Generate new refresh token. */ String newRawRefreshToken = generateRefreshToken();
		RefreshToken newRefreshToken = RefreshToken.builder().user(user).tokenHash(hashToken(newRawRefreshToken))
				.deviceId(currentToken.getDeviceId()).issuedAt(now)
				.expiresAt(now.plus(REFRESH_TOKEN_EXPIRY_DAYS, ChronoUnit.DAYS)).build();
		refreshTokenRepository.save(newRefreshToken);
		/* * Revoke old refresh token. */ currentToken.setRevokedAt(now);
		/* * Link old token -> replacement token. */ currentToken.setReplacedBy(newRefreshToken);
		refreshTokenRepository.save(currentToken);
		return new RefreshResponse(accessToken, newRawRefreshToken, "Bearer", ACCESS_TOKEN_EXPIRES_IN_SECONDS);
	}

	/** * LOGOUT CURRENT SESSION */
	public void logout(Long userId, String rawRefreshToken) {
		/*
		 * * No refresh cookie. * * Logout is already effectively complete * from the
		 * access-token perspective.
		 */ if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			return;
		}
		String tokenHash = hashToken(rawRefreshToken);
		refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
			/* * Only allow the authenticated user * to revoke their own token. */ if (token.getUser().getId()
					.equals(userId)) {
				token.setRevokedAt(Instant.now());
				refreshTokenRepository.save(token);
			}
		});
	}

	/** * LOGOUT ALL DEVICES */
	public void logoutAllDevices(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
		List<RefreshToken> tokens = refreshTokenRepository.findByUserAndRevokedAtIsNull(user);
		Instant now = Instant.now();
		/* * Revoke every active refresh token. */ tokens.forEach(token -> token.setRevokedAt(now));
		refreshTokenRepository.saveAll(tokens);
		/*
		 * * Invalidate already-issued access tokens. * * JwtValidationService must
		 * validate the * tokenVersion claim against the current user * version if
		 * immediate JWT invalidation is required.
		 */ user.setTokenVersion(user.getTokenVersion() + 1);
		userRepository.save(user);
	}

	/** * CURRENT USER */
	@Transactional(readOnly = true)
	public AuthUserResponse me(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
		String role = user.getRole().getName();
		List<String> permissions = user.getRole().getPermissions().stream().map(Permission::getCode).sorted().toList();
		return new AuthUserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), role,
				permissions);
	}

	/** * Validate account status. */
	private void validateAccountStatus(User user) {
		UserStatus status = user.getStatus();
		if (status == UserStatus.ACTIVE) {
			return;
		}
		if (status == UserStatus.LOCKED) {
			throw new AccountLockedException("User account is locked");
		}
		if (status == UserStatus.DISABLED) {
			throw new AccountDisabledException("User account is disabled");
		}
		if (status == UserStatus.PENDING) {
			throw new AccountDisabledException("User account is awaiting approval");
		}
		if (status == UserStatus.REJECTED) {
			throw new AccountDisabledException("User account registration was rejected");
		}
		/* * Defensive fallback. */ throw new AccountDisabledException("User account is not active");
	}

	/** * Generate cryptographically secure refresh token. */
	private String generateRefreshToken() {
		byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return "rt_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** * Store only SHA-256 hash of refresh token. */
	private String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (Exception ex) {
			/*
			 * * SHA-256 is required by the JVM. * * If the algorithm is unexpectedly
			 * unavailable, * this is an infrastructure/configuration failure, * not an
			 * authentication failure.
			 */ throw new IllegalStateException("Unable to hash refresh token", ex);
		}
	}
}