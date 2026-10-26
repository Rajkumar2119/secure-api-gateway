package com.rajkumar.gateway.auth.api;

import com.rajkumar.gateway.auth.domain.RoleName;
import com.rajkumar.gateway.auth.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Request and response bodies of the auth API. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                    message = "may contain letters, digits, '.', '_' and '-' only") String username,
            @NotBlank @Email @Size(max = 255) String email,
            // BCrypt only uses the first 72 bytes, so longer passwords are refused rather than truncated.
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {
    }

    public record RolesRequest(@NotEmpty Set<RoleName> roles) {
    }

    public record EnabledRequest(boolean enabled) {
    }

    public record UserResponse(Long id, String username, String email, List<String> roles, boolean enabled,
                               Instant createdAt) {

        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.roleNames(),
                    user.isEnabled(), user.getCreatedAt());
        }
    }

    public record MeResponse(String username, Long userId, List<String> roles) {
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    }
}
