package com.rajkumar.gateway.auth.service;

public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {
}
