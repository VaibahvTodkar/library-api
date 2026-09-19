package com.school.library.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.school.library.handler.SecurityAccessDeniedHandler;
import com.school.library.handler.SecurityExceptionHandler;
import com.school.library.security.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final SecurityExceptionHandler securityExceptionHandler;
	private final SecurityAccessDeniedHandler securityAccessDeniedHandler;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
			SecurityExceptionHandler securityExceptionHandler,
			SecurityAccessDeniedHandler securityAccessDeniedHandler) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.securityExceptionHandler = securityExceptionHandler;
		this.securityAccessDeniedHandler = securityAccessDeniedHandler;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http
				// CorsConfigDataSourceInstance Bean
				.cors(cors -> {
				})

				// JWT APIs are stateless
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// We are using Bearer tokens
				.csrf(csrf -> csrf.disable())

				.exceptionHandling(exception -> exception.authenticationEntryPoint(securityExceptionHandler)
						.accessDeniedHandler(securityAccessDeniedHandler))

				.authorizeHttpRequests(auth -> auth

						// Swagger / OpenAPI
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**")
						.permitAll()

						// Public authentication endpoints
						.requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh",
								"/api/v1/auth/forgot-password", "/api/v1/auth/reset-password")
						.permitAll()

						.requestMatchers("/api/v1/auth/logout", "/api/v1/auth/me").authenticated()

						// JWKS
						.requestMatchers("/.well-known/jwks.json").permitAll()

						// Health
						.requestMatchers("/actuator/health").permitAll()

						// Everything else requires authentication
						.anyRequest().authenticated())

				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
