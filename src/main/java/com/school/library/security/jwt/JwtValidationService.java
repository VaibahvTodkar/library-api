package com.school.library.security.jwt;

import java.security.PublicKey;
import java.util.List;

import org.springframework.stereotype.Service;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

@Service
public class JwtValidationService {

    private final JwtProperties properties;
    private final PublicKey publicKey;

    public JwtValidationService(
            JwtProperties properties,
            PublicKey publicKey
    ) {
        this.properties = properties;
        this.publicKey = publicKey;
    }

    public Claims validate(String token) {

        Jws<Claims> jwt =
                Jwts.parser()

                        .verifyWith(publicKey)

                        .requireIssuer(
                                properties.issuer()
                        )

                        .requireAudience(
                                properties.audience()
                        )

                        .clockSkewSeconds(
                                properties.clockSkew()
                                        .toSeconds()
                        )

                        .build()

                        .parseSignedClaims(token);

        return jwt.getPayload();
    }

    public String getRole(Claims claims) {

        return claims.get(
                properties.roleClaim(),
                String.class
        );
    }

    public List<String> getPermissions(Claims claims) {

        return claims.get(
                properties.permissionsClaim(),
                List.class
        );
    }

    public Long getUserId(Claims claims) {

        return claims.get(
                properties.userIdClaim(),
                Long.class
        );
    }

    public Integer getTokenVersion(Claims claims) {

        return claims.get(
                properties.tokenVersionClaim(),
                Integer.class
        );
    }
}


