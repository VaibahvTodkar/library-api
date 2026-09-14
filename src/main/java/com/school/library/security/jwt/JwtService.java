package com.school.library.security.jwt;

import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;


import io.jsonwebtoken.Jwts;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final PrivateKey privateKey;

    public JwtService(
            JwtProperties properties,
            PrivateKey privateKey
    ) {
        this.properties = properties;
        this.privateKey = privateKey;
    }

    public String generateAccessToken(
            Long userId,
            String username,
            String fullName,
            String email,
            String role,
            List<String> permissions,
            Integer tokenVersion
    ) {

        Instant now = Instant.now();

        Instant expiresAt =
                now.plus(properties.accessTokenTtl());

        return Jwts.builder()

                // Header
                .header()
                .keyId(properties.keyId())
                .type("JWT")
                .and()

                // Standard claims
                .issuer(properties.issuer())
                .subject(String.valueOf(userId))
                .audience()
                .add(properties.audience())
                .and()

                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(expiresAt))
                .id(UUID.randomUUID().toString())

                // Custom claims
                .claim(
                        properties.userIdClaim(),
                        userId
                )

                .claim(
                        "name",
                        fullName
                )

                .claim(
                        "email",
                        email
                )

                .claim(
                        properties.roleClaim(),
                        role
                )

                .claim(
                        properties.permissionsClaim(),
                        permissions
                )

                .claim(
                        properties.tokenVersionClaim(),
                        tokenVersion
                )

                // RS256
                .signWith(
                        privateKey,
                        Jwts.SIG.RS256
                )

                .compact();
    }
}

