package com.school.library.security.jwt;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/.well-known")
public class JwksController {

    private final JwtProperties jwtProperties;
    private final RSAPublicKey publicKey;

    public JwksController(
            JwtProperties jwtProperties,
            RSAPublicKey publicKey
    ) {
        this.jwtProperties = jwtProperties;
        this.publicKey = publicKey;
    }

    @GetMapping("/jwks.json")
    public ResponseEntity<Map<String, Object>> jwks() {

        Map<String, Object> key = Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", jwtProperties.algorithm(),
                "kid", jwtProperties.keyId(),
                "n", base64Url(publicKey.getModulus()),
                "e", base64Url(publicKey.getPublicExponent())
        );

        Map<String, Object> response = Map.of(
                "keys", new Object[] {
                        key
                }
        );

        return ResponseEntity.ok(response);
    }

    private String base64Url(BigInteger value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.toByteArray());
    }
}
