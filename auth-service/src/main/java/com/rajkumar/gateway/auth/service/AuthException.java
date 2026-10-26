package com.rajkumar.gateway.auth.service;

import org.springframework.http.HttpStatus;

/** A business error with the HTTP status it maps to. */
public class AuthException extends RuntimeException {

    private final HttpStatus status;

    private AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** Same message for unknown user and wrong password, so usernames can't be probed. */
    public static AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    public static AuthException invalidRefreshToken() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid, expired or already used");
    }

    public static AuthException conflict(String message) {
        return new AuthException(HttpStatus.CONFLICT, message);
    }

    public static AuthException notFound(String message) {
        return new AuthException(HttpStatus.NOT_FOUND, message);
    }

    public static AuthException badRequest(String message) {
        return new AuthException(HttpStatus.BAD_REQUEST, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
