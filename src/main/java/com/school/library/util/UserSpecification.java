package com.school.library.util;

import com.school.library.entity.User;
import com.school.library.enums.UserStatus;

import org.springframework.data.jpa.domain.Specification;

public final class UserSpecification {

    private UserSpecification() {
    }

    // =========================================================
    // SEARCH
    // =========================================================

    public static Specification<User> search(
            String search
    ) {

        return (root, query, cb) -> {

            if (search == null || search.isBlank()) {
                return null;
            }

            String value =
                    "%" + search.trim().toLowerCase() + "%";

            return cb.or(

                    cb.like(
                            cb.lower(
                                    root.get("username")
                            ),
                            value
                    ),

                    cb.like(
                            cb.lower(
                                    root.get("email")
                            ),
                            value
                    ),

                    cb.like(
                            cb.lower(
                                    root.get("fullName")
                            ),
                            value
                    )
            );
        };
    }

    // =========================================================
    // STATUS
    // =========================================================

    public static Specification<User> hasStatus(
            UserStatus status
    ) {

        return (root, query, cb) -> {

            if (status == null) {
                return null;
            }

            return cb.equal(
                    root.get("status"),
                    status
            );
        };
    }

    // =========================================================
    // ROLE
    // =========================================================

    public static Specification<User> hasRoleId(
            Long roleId
    ) {

        return (root, query, cb) -> {

            if (roleId == null) {
                return null;
            }

            return cb.equal(
                    root.get("role").get("id"),
                    roleId
            );
        };
    }

    // =========================================================
    // USERNAME
    // =========================================================

    public static Specification<User> usernameContains(
            String username
    ) {

        return (root, query, cb) -> {

            if (username == null || username.isBlank()) {
                return null;
            }

            return cb.like(
                    cb.lower(root.get("username")),
                    "%" + username.trim().toLowerCase() + "%"
            );
        };
    }

    // =========================================================
    // EMAIL
    // =========================================================

    public static Specification<User> emailContains(
            String email
    ) {

        return (root, query, cb) -> {

            if (email == null || email.isBlank()) {
                return null;
            }

            return cb.like(
                    cb.lower(root.get("email")),
                    "%" + email.trim().toLowerCase() + "%"
            );
        };
    }
}