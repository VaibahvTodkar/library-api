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
import com.school.library.repository.PasswordResetTokenRepository;
import com.school.library.repository.UserRepository;

@Service
@Transactional
public class PasswordResetService {

    private static final int TOKEN_BYTES = 64;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom =
            new SecureRandom();

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
     */
    public void requestPasswordReset(
            String email
    ) {

        String normalizedEmail =
                email.trim().toLowerCase();

        userRepository
                .findByEmail(normalizedEmail)
                .ifPresent(user -> {

                    /*
                     * Remove previous reset tokens.
                     */
                    tokenRepository.deleteByUser(user);

                    String rawToken =
                            generateToken();

                    PasswordResetToken resetToken =
                            PasswordResetToken.builder()
                                    .user(user)
                                    .tokenHash(
                                            hashToken(rawToken)
                                    )
                                    .createdAt(
                                            Instant.now()
                                    )
                                    .expiresAt(
                                            Instant.now()
                                                    .plus(
                                                            15,
                                                            ChronoUnit.MINUTES
                                                    )
                                    )
                                    .build();

                    tokenRepository.save(resetToken);

                    /*
                     * TODO:
                     *
                     * Send email:
                     *
                     * https://frontend.school.com/reset-password?token=<rawToken>
                     *
                     * Never store rawToken in the database.
                     */
                    System.out.println(
                            "PASSWORD RESET TOKEN: "
                                    + rawToken
                    );
                });
    }

    /**
     * Reset password.
     */
    public void resetPassword(
            String rawToken,
            String newPassword
    ) {

        String tokenHash =
                hashToken(rawToken);

        PasswordResetToken resetToken =
                tokenRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid or expired reset token"
                                )
                        );

        if (resetToken.getUsedAt() != null) {

            throw new IllegalArgumentException(
                    "Reset token has already been used"
            );
        }

        if (resetToken.getExpiresAt()
                .isBefore(Instant.now())) {

            throw new IllegalArgumentException(
                    "Reset token has expired"
            );
        }

        User user =
                resetToken.getUser();

        user.setPasswordHash(
                passwordEncoder.encode(
                        newPassword
                )
        );

        /*
         * Invalidate existing JWTs if your JWT validation
         * checks tokenVersion.
         */
        user.setTokenVersion(
                user.getTokenVersion() + 1
        );

        /*
         * Revoke all refresh sessions.
         */
        user.getRefreshTokens()
                .forEach(token ->
                        token.setRevokedAt(
                                Instant.now()
                        )
                );

        userRepository.save(user);

        resetToken.setUsedAt(
                Instant.now()
        );

        tokenRepository.save(resetToken);
    }

    private String generateToken() {

        byte[] bytes =
                new byte[TOKEN_BYTES];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashToken(
            String token
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            token.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of().formatHex(hash);

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Unable to hash password reset token",
                    ex
            );
        }
    }
}