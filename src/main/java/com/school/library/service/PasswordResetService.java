package com.school.library.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.school.library.entity.PasswordResetToken;
import com.school.library.entity.User;
import com.school.library.exception.InvalidPasswordResetTokenException;
import com.school.library.exception.PasswordResetTokenUsedException;
import com.school.library.exception.TokenExpiredException;
import com.school.library.repository.PasswordResetTokenRepository;
import com.school.library.repository.UserRepository;

@Service
@Transactional
public class PasswordResetService {

    private static final int TOKEN_BYTES = 64;
    private static final int TOKEN_EXPIRY_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Request password reset.
     *
     * IMPORTANT:
     * In production this should send the token
     * through an EmailService.
     *
     * This method intentionally does not throw an exception
     * when the email does not exist to prevent account enumeration.
     */
    public void requestPasswordReset(String email) {

        String normalizedEmail = email.trim().toLowerCase();

        userRepository
                .findByEmail(normalizedEmail)
                .ifPresent(user -> {

                    /*
                     * Remove previous reset tokens.
                     */
                    tokenRepository.deleteByUser(user);

                    String rawToken = generateToken();

                    Instant now = Instant.now();

                    PasswordResetToken resetToken =
                            PasswordResetToken.builder()
                                    .user(user)
                                    .tokenHash(hashToken(rawToken))
                                    .createdAt(now)
                                    .expiresAt(
                                            now.plus(
                                                    TOKEN_EXPIRY_MINUTES,
                                                    ChronoUnit.MINUTES
                                            )
                                    )
                                    .build();

                    tokenRepository.save(resetToken);

                    /*
                     * TODO:
                     *
                     * Send email through EmailService:
                     *
                     * https://frontend.school.com/reset-password?token=<rawToken>
                     *
                     * NEVER store rawToken in the database.
                     */
                    System.out.println(
                            "PASSWORD RESET TOKEN: " + rawToken
                    );
                });
    }

    /**
     * Reset password using a valid password reset token.
     */
    public void resetPassword(
            String rawToken,
            String newPassword
    ) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidPasswordResetTokenException(
                    "Password reset token is required"
            );
        }

        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken =
                tokenRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new InvalidPasswordResetTokenException(
                                        "Invalid password reset token"
                                )
                        );

        /*
         * Token must be single-use.
         */
        if (resetToken.getUsedAt() != null) {
            throw new PasswordResetTokenUsedException();
        }

        /*
         * Token expiration.
         */
        if (resetToken.getExpiresAt() == null
                || resetToken.getExpiresAt().isBefore(Instant.now())) {

            throw new TokenExpiredException(
                    "Password reset token has expired"
            );
        }

        User user = resetToken.getUser();

        /*
         * Update password.
         */
        user.setPasswordHash(
                passwordEncoder.encode(newPassword)
        );

        /*
         * Invalidate existing JWTs if JWT validation
         * checks tokenVersion.
         */
        user.setTokenVersion(
                user.getTokenVersion() + 1
        );

        /*
         * Revoke all refresh sessions.
         */
        Instant now = Instant.now();

        user.getRefreshTokens()
                .forEach(token ->
                        token.setRevokedAt(now)
                );

        userRepository.save(user);

        /*
         * Mark reset token as consumed.
         */
        resetToken.setUsedAt(now);

        tokenRepository.save(resetToken);
    }

    /**
     * Generates a cryptographically secure random token.
     */
    private String generateToken() {

        byte[] bytes = new byte[TOKEN_BYTES];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    /**
     * SHA-256 hash of the reset token.
     *
     * Only the hash is stored in the database.
     */
    private String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            token.getBytes(StandardCharsets.UTF_8)
                    );

            return HexFormat.of().formatHex(hash);

        } catch (Exception ex) {

            /*
             * This is an infrastructure/JVM-level failure,
             * not a client/business validation error.
             */
            throw new IllegalStateException(
                    "Unable to hash password reset token",
                    ex
            );
        }
    }
}
