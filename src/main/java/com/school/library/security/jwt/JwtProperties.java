package com.school.library.security.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String issuer, String audience, Duration accessTokenTtl, Duration clockSkew,
		String privateKey, String publicKey, String keyId, String algorithm, String roleClaim, String permissionsClaim,
		String userIdClaim, String tokenVersionClaim) {
	public JwtProperties {
		if (issuer == null || issuer.isBlank()) {
			throw new IllegalArgumentException("JWT issuer must not be empty");
		}
		if (audience == null || audience.isBlank()) {
			throw new IllegalArgumentException("JWT audience must not be empty");
		}
		if (accessTokenTtl == null) {
			accessTokenTtl = Duration.ofMinutes(15);
		}
		if (clockSkew == null) {
			clockSkew = Duration.ofSeconds(60);
		}
		if (algorithm == null || algorithm.isBlank()) {
			algorithm = "RS256";
		}
		if (keyId == null || keyId.isBlank()) {
			throw new IllegalArgumentException("JWT keyId must not be empty");
		}
		if (roleClaim == null || roleClaim.isBlank()) {
			roleClaim = "role";
		}
		if (permissionsClaim == null || permissionsClaim.isBlank()) {
			permissionsClaim = "permissions";
		}
		if (userIdClaim == null || userIdClaim.isBlank()) {
			userIdClaim = "uid";
		}
		if (tokenVersionClaim == null || tokenVersionClaim.isBlank()) {
			tokenVersionClaim = "token_version";
		}
	}
}
