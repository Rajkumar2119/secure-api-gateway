package com.rajkumar.gateway.auth;

import com.rajkumar.gateway.auth.config.JwtProperties;
import com.rajkumar.gateway.auth.domain.Role;
import com.rajkumar.gateway.auth.domain.RoleName;
import com.rajkumar.gateway.auth.domain.RoleRepository;
import com.rajkumar.gateway.auth.domain.User;
import com.rajkumar.gateway.auth.domain.UserRepository;
import com.rajkumar.gateway.auth.service.AuthException;
import com.rajkumar.gateway.auth.service.AuthService;
import com.rajkumar.gateway.auth.service.TokenPair;
import com.rajkumar.gateway.auth.token.AccessTokenService;
import com.rajkumar.gateway.auth.token.RefreshTokenStore;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private RoleRepository roles;

    @Mock
    private AccessTokenService accessTokens;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final InMemoryRefreshTokenStore refreshStore = new InMemoryRefreshTokenStore();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties("unit-test-secret-0123456789abcdef0123", "test", null, null);
        authService = new AuthService(users, roles, passwordEncoder, accessTokens, refreshStore, props,
                new SimpleMeterRegistry());
    }

    private User existingUser(String rawPassword) {
        User user = new User("alice", "alice@example.com", passwordEncoder.encode(rawPassword),
                Set.of(new Role((short) 1, RoleName.USER)));
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }

    @Test
    void registerHashesPasswordAndAssignsUserRoleOnly() {
        when(roles.findByName(RoleName.USER)).thenReturn(Optional.of(new Role((short) 1, RoleName.USER)));
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.register("bob", "  Bob@Example.COM ", "s3cret-password");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("s3cret-password");
        assertThat(passwordEncoder.matches("s3cret-password", captor.getValue().getPasswordHash())).isTrue();
        assertThat(saved.getEmail()).isEqualTo("bob@example.com");
        assertThat(saved.roleNames()).containsExactly("USER");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(users.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("alice", "a@example.com", "s3cret-password"))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(users, never()).save(any());
    }

    @Test
    void loginWithCorrectPasswordIssuesAccessAndRefreshTokens() {
        User alice = existingUser("correct-horse");
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(accessTokens.issue(alice)).thenReturn("jwt-token");
        when(accessTokens.accessTokenTtlSeconds()).thenReturn(900L);

        TokenPair pair = authService.login("alice", "correct-horse");

        assertThat(pair.accessToken()).isEqualTo("jwt-token");
        assertThat(pair.refreshToken()).hasSizeGreaterThan(40);
        assertThat(pair.expiresInSeconds()).isEqualTo(900);
        assertThat(refreshStore.tokens).hasSize(1).doesNotContainKey(pair.refreshToken()); // only the hash is stored
    }

    @Test
    void loginWithWrongPasswordOrUnknownUserGivesTheSameError() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(existingUser("correct-horse")));
        when(users.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("alice", "wrong"))
                .isInstanceOf(AuthException.class).hasMessage("Invalid username or password");
        assertThatThrownBy(() -> authService.login("nobody", "wrong"))
                .isInstanceOf(AuthException.class).hasMessage("Invalid username or password");
    }

    @Test
    void disabledUserCannotLogIn() {
        User alice = existingUser("correct-horse");
        alice.setEnabled(false);
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));

        assertThatThrownBy(() -> authService.login("alice", "correct-horse")).isInstanceOf(AuthException.class);
    }

    @Test
    void refreshRotatesTokenAndOldTokenCannotBeReused() {
        User alice = existingUser("correct-horse");
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(users.findById(7L)).thenReturn(Optional.of(alice));
        when(accessTokens.issue(alice)).thenReturn("jwt-token");

        TokenPair first = authService.login("alice", "correct-horse");
        TokenPair second = authService.refresh(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThatThrownBy(() -> authService.refresh(first.refreshToken()))
                .isInstanceOf(AuthException.class)
                .extracting(e -> ((AuthException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutRevokesRefreshToken() {
        User alice = existingUser("correct-horse");
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(accessTokens.issue(alice)).thenReturn("jwt-token");

        TokenPair pair = authService.login("alice", "correct-horse");
        authService.logout(pair.refreshToken());

        assertThatThrownBy(() -> authService.refresh(pair.refreshToken())).isInstanceOf(AuthException.class);
    }

    /** Behaves like the Redis store (consume = GETDEL) without needing Redis. */
    static class InMemoryRefreshTokenStore implements RefreshTokenStore {

        final Map<String, Long> tokens = new HashMap<>();

        @Override
        public void save(String tokenHash, long userId, Duration ttl) {
            tokens.put(tokenHash, userId);
        }

        @Override
        public Optional<Long> consume(String tokenHash) {
            return Optional.ofNullable(tokens.remove(tokenHash));
        }

        @Override
        public void revoke(String tokenHash) {
            tokens.remove(tokenHash);
        }
    }
}
