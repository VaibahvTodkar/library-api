package com.school.library.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "School Library Management System API",
                version = "v1.0.0",
                description = """
                        REST API for the School Library Management System.

                        Provides APIs for:

                        - User registration and authentication
                        - JWT access and refresh token management
                        - Role-based access control
                        - User management
                        - Book management
                        - Book borrowing and returning
                        - Library operations
                        - Account status management

                        Authentication:
                        Most APIs require a valid JWT access token.

                        Authorization:
                        Access to protected APIs is controlled using
                        roles and permissions.
                        """,
                contact = @Contact(
                        name = "Library API Team",
                        email = "support@school-library.local"
                ),
                license = @License(
                        name = "Internal Use"
                )
        ),

        servers = {

                @Server(
                        url = "http://localhost:8080",
                        description = "Local Development"
                ),

//                @Server(
//                        url = "https://staging-api.school-library.example",
//                        description = "Staging Environment"
//                ),
//
//                @Server(
//                        url = "https://api.school-library.example",
//                        description = "Production Environment"
//                )
        }
        
//        tags = {
//
//                @Tag(
//                        name = "Authentication",
//                        description = "User registration, login, logout, token refresh and password recovery"
//                ),
//
//                @Tag(
//                        name = "Users",
//                        description = "User management and account administration"
//                ),
//
//                @Tag(
//                        name = "Books",
//                        description = "Book management and catalogue operations"
//                ),
//
//                @Tag(
//                        name = "Loans",
//                        description = "Book borrowing, issuing, returning and loan management"
//                ),
//
//                @Tag(
//                        name = "Library",
//                        description = "Library management operations"
//                ),
//
//                @Tag(
//                        name = "Health",
//                        description = "Application health and monitoring endpoints"
//                )
//        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = """
                JWT Bearer authentication.

                Enter only the access token.

                Example:

                eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
                """
)
public class OpenApiConfig {
}