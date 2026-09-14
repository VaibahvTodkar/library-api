package com.school.library.security.jwt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtValidationService jwtValidationService;

    public JwtAuthenticationFilter(
            JwtValidationService jwtValidationService
    ) {
        this.jwtValidationService = jwtValidationService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization =
                request.getHeader("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authorization.substring(7);

        try {

            Claims claims =
                    jwtValidationService.validate(token);

            Long userId =
                    jwtValidationService.getUserId(claims);

            String role =
                    jwtValidationService.getRole(claims);

            List<String> permissions =
                    jwtValidationService.getPermissions(claims);

            List<SimpleGrantedAuthority> authorities =
                    new ArrayList<>();

            // ROLE_LIBRARIAN
            authorities.add(
                    new SimpleGrantedAuthority(
                            "ROLE_" + role
                    )
            );

            // books:read
            // books:write
            // loans:issue
            if (permissions != null) {

                permissions.forEach(permission ->
                        authorities.add(
                                new SimpleGrantedAuthority(
                                        permission
                                )
                        )
                );
            }

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

        } catch (Exception ex) {

            SecurityContextHolder.clearContext();

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType(
                    "application/json"
            );

            response.getWriter().write("""
                    {
                      "status": 401,
                      "error": "Unauthorized",
                      "message": "Invalid or expired access token"
                    }
                    """);

            return;
        }

        filterChain.doFilter(request, response);
    }
}

