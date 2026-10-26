package com.rajkumar.gateway.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Token settings shared by every service through the same environment variables.
 *
 * @param secret          HS256 signing key, at least 32 bytes
 * @param issuer          value of the {@code iss} claim; resource servers reject other issuers
 * @param accessTokenTtl  lifetime of a JWT access token (short, because it can't be revoked)
 * @param refreshTokenTtl lifetime of an opaque refresh token stored in Redis
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @NotBlank String issuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl) {

    public JwtProperties {
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(15);
        }
        if (refreshTokenTtl == null) {
            refreshTokenTtl = Duration.ofDays(7);
        }
    }
}
