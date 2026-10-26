package com.rajkumar.gateway.auth;

import com.rajkumar.gateway.auth.api.AuthDtos.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Whole auth flow against real MySQL 8.4 and Redis 7 in Docker (Testcontainers):
 * Flyway migrations, BCrypt, JWT issuing and refresh-token rotation in Redis.
 * Runs with `mvn verify` (failsafe) and in GitHub Actions; skipped when Docker is missing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.bootstrap-admin.username=root-admin", "app.bootstrap-admin.password=Admin-pass-123"})
@Testcontainers(disabledWithoutDocker = true)
class AuthFlowContainersIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void registerLoginRefreshAndRotate() {
        ResponseEntity<Map> registered = http.postForEntity("/api/v1/auth/register",
                Map.of("username", "carol", "email", "carol@example.com", "password", "carol-password"), Map.class);
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registered.getBody()).containsEntry("roles", java.util.List.of("USER"));

        ResponseEntity<Map> duplicate = http.postForEntity("/api/v1/auth/register",
                Map.of("username", "carol", "email", "other@example.com", "password", "carol-password"), Map.class);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        TokenResponse login = http.postForObject("/api/v1/auth/login",
                Map.of("username", "carol", "password", "carol-password"), TokenResponse.class);
        assertThat(login.accessToken()).isNotBlank();
        assertThat(redisTemplate.keys("refresh:*")).isNotEmpty();

        ResponseEntity<Map> me = http.exchange("/api/v1/auth/me", HttpMethod.GET, bearer(login.accessToken()), Map.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).containsEntry("username", "carol");

        TokenResponse refreshed = http.postForObject("/api/v1/auth/refresh",
                Map.of("refreshToken", login.refreshToken()), TokenResponse.class);
        assertThat(refreshed.refreshToken()).isNotEqualTo(login.refreshToken());

        ResponseEntity<Map> reuse = http.postForEntity("/api/v1/auth/refresh",
                Map.of("refreshToken", login.refreshToken()), Map.class);
        assertThat(reuse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void onlyAdminsCanUseTheAdminApi() {
        http.postForEntity("/api/v1/auth/register",
                Map.of("username", "dave", "email", "dave@example.com", "password", "dave-password"), Map.class);
        String userToken = http.postForObject("/api/v1/auth/login",
                Map.of("username", "dave", "password", "dave-password"), TokenResponse.class).accessToken();
        String adminToken = http.postForObject("/api/v1/auth/login",
                Map.of("username", "root-admin", "password", "Admin-pass-123"), TokenResponse.class).accessToken();

        assertThat(http.exchange("/api/v1/admin/users", HttpMethod.GET, bearer(userToken), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(http.exchange("/api/v1/admin/users", HttpMethod.GET, bearer(adminToken), Map.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    private static HttpEntity<Void> bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}
