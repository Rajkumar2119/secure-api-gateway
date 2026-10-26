package com.rajkumar.gateway.edge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis-backed rate limiting against a real Redis 7 (Testcontainers). The login route
 * allows a burst of 3, so a fast burst of 10 requests from one IP must hit 429.
 */
@SpringBootTest(properties = {
        "app.security.jwt.secret=" + TestTokens.SECRET,
        "app.gateway.auth-service-uri=http://localhost:1",
        "app.gateway.catalog-service-uri=http://localhost:1",
        "app.gateway.rate-limit.enabled=true",
        "app.gateway.rate-limit.auth-replenish-rate=1",
        "app.gateway.rate-limit.auth-burst-capacity=3"
})
@AutoConfigureWebTestClient
@Testcontainers(disabledWithoutDocker = true)
class RateLimitingContainersIT {

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    @Autowired
    private WebTestClient client;

    @Test
    void burstOfLoginAttemptsFromOneIpIsThrottled() {
        List<HttpStatusCode> statuses = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            statuses.add(client.post().uri("/api/v1/auth/login")
                    .header("X-Forwarded-For", "203.0.113.7")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"username\":\"alice\",\"password\":\"guess-" + i + "\"}")
                    .exchange()
                    .returnResult(String.class)
                    .getStatus());
        }

        assertThat(statuses).contains(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(statuses.stream().filter(s -> s.value() == 429).count()).isGreaterThanOrEqualTo(5);
    }
}
