package com.school.library.dto.response;

import java.util.List;

public record AuthUserResponse(

        Long id,

        String username,

        String email,

        String fullName,

        String role,

        List<String> permissions
) {
}
