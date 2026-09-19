package com.school.library.security.jwt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.school.library.handler.SecurityExceptionHandler;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtValidationService jwtValidationService;
    private final SecurityExceptionHandler securityExceptionHandler;

    public JwtAuthenticationFilter(
            JwtValidationService jwtValidationService,
            SecurityExceptionHandler securityExceptionHandler
    ) {
        this.jwtValidationService = jwtValidationService;
        this.securityExceptionHandler = securityExceptionHandler;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        /*
         * No Authorization header.
         *
         * Do not reject here.
         * Spring Security will decide whether the endpoint
         * requires authentication.
         */
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7).trim();

        /*
         * Empty Bearer token.
         */
        if (token.isBlank()) {

            SecurityContextHolder.clearContext();

            securityExceptionHandler.commence(
                    request,
                    response,
                    new BadCredentialsException(
                            "Access token is missing"
                    )
            );

            return;
        }

        try {

            /*
             * Validate JWT signature, expiration,
             * issuer, audience, etc.
             */
            Claims claims =
                    jwtValidationService.validate(token);

            Long userId =
                    jwtValidationService.getUserId(claims);

            String role =
                    jwtValidationService.getRole(claims);

            List<String> permissions =
                    jwtValidationService.getPermissions(claims);

            /*
             * Build authorities.
             */
            List<SimpleGrantedAuthority> authorities =
                    new ArrayList<>();

            /*
             * Example:
             *
             * ROLE_ADMIN
             * ROLE_LIBRARIAN
             * ROLE_TEACHER
             * ROLE_STUDENT
             */
            if (role != null && !role.isBlank()) {

                authorities.add(
                        new SimpleGrantedAuthority(
                                "ROLE_" + role
                        )
                );
            }

            /*
             * Example:
             *
             * users:read
             * users:write
             * users:delete
             * books:read
             * books:write
             * loans:issue
             */
            if (permissions != null) {

                permissions.stream()
                        .filter(permission ->
                                permission != null
                                        && !permission.isBlank()
                        )
                        .map(SimpleGrantedAuthority::new)
                        .forEach(authorities::add);
            }

            /*
             * Create authenticated principal.
             *
             * Principal = userId
             */
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContextHolder
                    .getContext()
                    .setAuthentication(authentication);

        } catch (ExpiredJwtException ex) {

            /*
             * JWT exists but has expired.
             */
            SecurityContextHolder.clearContext();

            securityExceptionHandler.commence(
                    request,
                    response,
                    new CredentialsExpiredException(
                            "Access token has expired"
                    )
            );

            return;

        } catch (Exception ex) {

            /*
             * Invalid signature, malformed JWT,
             * invalid claims, invalid key, etc.
             */
            SecurityContextHolder.clearContext();

            securityExceptionHandler.commence(
                    request,
                    response,
                    new BadCredentialsException(
                            "Invalid access token",
                            ex
                    )
            );

            return;
        }

        /*
         * JWT successfully validated.
         */
        filterChain.doFilter(request, response);
    }
}