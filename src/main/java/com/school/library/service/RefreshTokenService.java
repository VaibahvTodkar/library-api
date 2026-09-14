package com.school.library.service;

import com.school.library.entity.RefreshToken;
import com.school.library.entity.User;
import com.school.library.repository.RefreshTokenRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@Transactional
public class RefreshTokenService {

	private static final int TOKEN_BYTES = 64;

	private final RefreshTokenRepository refreshTokenRepository;

	private final SecureRandom secureRandom;

	private final long refreshTokenTtlDays;

	public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
			@Value("${app.jwt.refresh-token-ttl-days:7}") long refreshTokenTtlDays) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.refreshTokenTtlDays = refreshTokenTtlDays;
		this.secureRandom = new SecureRandom();
	}

	// =========================================================
	// CREATE
	// =========================================================

	public RefreshTokenResult create(User user, String deviceId, String userAgent, String ip) {

		String rawToken = generateToken();

		String tokenHash = hashToken(rawToken);

		Instant issuedAt = Instant.now();

		Instant expiresAt = issuedAt.plus(refreshTokenTtlDays, ChronoUnit.DAYS);

		RefreshToken refreshToken = RefreshToken.builder().user(user).tokenHash(tokenHash).deviceId(deviceId)
				.userAgent(userAgent).ip(ip).issuedAt(issuedAt).expiresAt(expiresAt).build();

		refreshTokenRepository.save(refreshToken);

		return new RefreshTokenResult(rawToken, refreshToken);
	}

	// =========================================================
	// VALIDATE
	// =========================================================

	@Transactional(readOnly = true)
	public RefreshToken validate(String rawToken) {

		String tokenHash = hashToken(rawToken);

		RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
				.orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

		if (refreshToken.getRevokedAt() != null) {

			throw new IllegalArgumentException("Refresh token has been revoked");
		}

		if (refreshToken.getExpiresAt().isBefore(Instant.now())) {

			throw new IllegalArgumentException("Refresh token has expired");
		}

		return refreshToken;
	}

	// =========================================================
	// ROTATE
	// =========================================================

	public RefreshTokenResult rotate(String rawToken, String deviceId, String userAgent, String ip) {

		RefreshToken oldToken = validate(rawToken);

		/*
		 * Create the replacement token first.
		 */
		RefreshTokenResult newToken = create(oldToken.getUser(), deviceId, userAgent, ip);

		/*
		 * Revoke old token.
		 */
		oldToken.setRevokedAt(Instant.now());

		/*
		 * Link old token -> new token.
		 */
		oldToken.setReplacedBy(newToken.refreshToken());

		refreshTokenRepository.save(oldToken);

		return newToken;
	}

	// =========================================================
	// REVOKE
	// =========================================================

	public void revoke(String rawToken) {

		String tokenHash = hashToken(rawToken);

		refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {

			if (token.getRevokedAt() == null) {

				token.setRevokedAt(Instant.now());

				refreshTokenRepository.save(token);
			}
		});
	}

	// =========================================================
	// GENERATE RAW TOKEN
	// =========================================================

	private String generateToken() {

		byte[] bytes = new byte[TOKEN_BYTES];

		secureRandom.nextBytes(bytes);

		return "rt_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	// =========================================================
	// SHA-256
	// =========================================================

	private String hashToken(String rawToken) {

		try {

			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

			return bytesToHex(hash);

		} catch (NoSuchAlgorithmException ex) {

			throw new IllegalStateException("SHA-256 algorithm is not available", ex);
		}
	}

	private String bytesToHex(byte[] bytes) {

		StringBuilder result = new StringBuilder(bytes.length * 2);

		for (byte b : bytes) {

			result.append(String.format("%02x", b));
		}

		return result.toString();
	}

	// =========================================================
	// RESULT
	// =========================================================

	public record RefreshTokenResult(String rawToken, RefreshToken refreshToken) {
	}
}