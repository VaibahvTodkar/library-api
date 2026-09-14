package com.school.library.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
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
import com.school.library.repository.RefreshTokenRepository;
import com.school.library.repository.RoleRepository;
import com.school.library.repository.UserRepository;
import com.school.library.security.jwt.JwtService;

@Service
@Transactional
public class AuthenticationService {

    private static final int REFRESH_TOKEN_BYTES = 64;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthenticationService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }
    
    /**
	 * REGISTER
	 */
    @Transactional
    public RegisterUserResponse registerUser(RegisterUserRequest request) {

        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Role studentRole = roleRepository
                .findByName("STUDENT")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "STUDENT role is not configured"
                        )
                );

        User user = User.builder()
                .username(request.username())
                .passwordHash(
                        passwordEncoder.encode(request.password())
                )
                .email(request.email())
                .fullName(request.fullName())
                .role(studentRole)
                .status(UserStatus.PENDING)
                .tokenVersion(0)
                .failedAttempts(0)
                .build();

        user = userRepository.save(user);

        return new RegisterUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().getName(),
                user.getStatus().name()
        );
    }

    /**
     * LOGIN
     */
    public LoginResponse login(
            String username,
            String password,
            String deviceId
    ) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid username or password"
                        )
                );

        if (user.getStatus() != UserStatus.ACTIVE) {

            throw new DisabledException(
                    "User account is not active"
            );
        }

        if (!passwordEncoder.matches(
                password,
                user.getPasswordHash()
        )) {

            throw new BadCredentialsException(
                    "Invalid username or password"
            );
        }

        String role = user.getRole().getName();

        List<String> permissions =
                user.getRole()
                        .getPermissions()
                        .stream()
                        .map(Permission::getCode)
                        .sorted()
                        .toList();

        String accessToken =
                jwtService.generateAccessToken(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName(),
                        user.getEmail(),
                        role,
                        permissions,
                        user.getTokenVersion()
                );

        String rawRefreshToken =
                generateRefreshToken();

        RefreshToken refreshToken =
                RefreshToken.builder()
                        .user(user)
                        .tokenHash(hashToken(rawRefreshToken))
                        .deviceId(deviceId)
                        .issuedAt(Instant.now())
                        .expiresAt(
                                Instant.now()
                                        .plus(7, ChronoUnit.DAYS)
                        )
                        .build();

        refreshTokenRepository.save(refreshToken);

        user.setLastLoginAt(Instant.now());

        userRepository.save(user);

        return new LoginResponse(
                accessToken,
                "Bearer",
                900,
                rawRefreshToken
        );
    }

    /**
     * REFRESH TOKEN
     */
    public RefreshResponse refresh(
            String rawRefreshToken
    ) {

        if (rawRefreshToken == null ||
                rawRefreshToken.isBlank()) {

            throw new BadCredentialsException(
                    "Refresh token is required"
            );
        }

        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken currentToken =
                refreshTokenRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new BadCredentialsException(
                                        "Invalid refresh token"
                                )
                        );

        Instant now = Instant.now();

        if (currentToken.getRevokedAt() != null) {

            throw new BadCredentialsException(
                    "Refresh token has been revoked"
            );
        }

        if (currentToken.getExpiresAt().isBefore(now)) {

            throw new BadCredentialsException(
                    "Refresh token has expired"
            );
        }

        User user = currentToken.getUser();

        if (user.getStatus() != UserStatus.ACTIVE) {

            throw new DisabledException(
                    "User account is not active"
            );
        }

        String role = user.getRole().getName();

        List<String> permissions =
                user.getRole()
                        .getPermissions()
                        .stream()
                        .map(Permission::getCode)
                        .sorted()
                        .toList();

        String accessToken =
                jwtService.generateAccessToken(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName(),
                        user.getEmail(),
                        role,
                        permissions,
                        user.getTokenVersion()
                );

        String newRawRefreshToken =
                generateRefreshToken();

        RefreshToken newRefreshToken =
                RefreshToken.builder()
                        .user(user)
                        .tokenHash(
                                hashToken(newRawRefreshToken)
                        )
                        .deviceId(
                                currentToken.getDeviceId()
                        )
                        .issuedAt(now)
                        .expiresAt(
                                now.plus(7, ChronoUnit.DAYS)
                        )
                        .build();

        refreshTokenRepository.save(newRefreshToken);

        currentToken.setRevokedAt(now);
        currentToken.setReplacedBy(newRefreshToken);

        refreshTokenRepository.save(currentToken);

        return new RefreshResponse(
                accessToken,
                newRawRefreshToken,
                "Bearer",
                900
        );
    }

    /**
     * LOGOUT CURRENT SESSION
     */
    public void logout(
            Long userId,
            String rawRefreshToken
    ) {

        if (rawRefreshToken == null ||
                rawRefreshToken.isBlank()) {
            return;
        }

        String tokenHash =
                hashToken(rawRefreshToken);

        refreshTokenRepository
                .findByTokenHash(tokenHash)
                .ifPresent(token -> {

                    if (token.getUser().getId()
                            .equals(userId)) {

                        token.setRevokedAt(
                                Instant.now()
                        );

                        refreshTokenRepository.save(token);
                    }
                });
    }

    /**
     * LOGOUT ALL DEVICES
     */
    public void logoutAllDevices(
            Long userId
    ) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new BadCredentialsException(
                                        "User not found"
                                )
                        );

        List<RefreshToken> tokens =
                refreshTokenRepository
                        .findByUserAndRevokedAtIsNull(user);

        Instant now = Instant.now();

        tokens.forEach(token ->
                token.setRevokedAt(now)
        );

        refreshTokenRepository.saveAll(tokens);

        /*
         * Invalidate already issued JWTs when your JWT
         * validation checks tokenVersion.
         */
        user.setTokenVersion(
                user.getTokenVersion() + 1
        );

        userRepository.save(user);
    }

    /**
     * CURRENT USER
     */
    @Transactional(readOnly = true)
    public AuthUserResponse me(
            Long userId
    ) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new BadCredentialsException(
                                        "User not found"
                                )
                        );

        String role =
                user.getRole().getName();

        List<String> permissions =
                user.getRole()
                        .getPermissions()
                        .stream()
                        .map(Permission::getCode)
                        .sorted()
                        .toList();

        return new AuthUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                role,
                permissions
        );
    }

    /**
     * Generate cryptographically secure refresh token.
     */
    private String generateRefreshToken() {

        byte[] bytes =
                new byte[REFRESH_TOKEN_BYTES];

        secureRandom.nextBytes(bytes);

        return "rt_" +
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(bytes);
    }

    /**
     * Store only SHA-256 hash of refresh token.
     */
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
                    "Unable to hash token",
                    ex
            );
        }
    }
}