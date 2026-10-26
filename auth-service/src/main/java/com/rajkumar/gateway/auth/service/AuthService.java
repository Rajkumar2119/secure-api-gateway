package com.rajkumar.gateway.auth.service;

import com.rajkumar.gateway.auth.config.JwtProperties;
import com.rajkumar.gateway.auth.domain.Role;
import com.rajkumar.gateway.auth.domain.RoleName;
import com.rajkumar.gateway.auth.domain.RoleRepository;
import com.rajkumar.gateway.auth.domain.User;
import com.rajkumar.gateway.auth.domain.UserRepository;
import com.rajkumar.gateway.auth.token.AccessTokenService;
import com.rajkumar.gateway.auth.token.RefreshTokenStore;
import com.rajkumar.gateway.auth.token.RefreshTokens;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokens;
    private final RefreshTokenStore refreshTokens;
    private final JwtProperties jwtProperties;
    private final MeterRegistry meterRegistry;

    public AuthService(UserRepository users, RoleRepository roles, PasswordEncoder passwordEncoder,
                       AccessTokenService accessTokens, RefreshTokenStore refreshTokens,
                       JwtProperties jwtProperties, MeterRegistry meterRegistry) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.jwtProperties = jwtProperties;
        this.meterRegistry = meterRegistry;
    }

    /** New accounts always get the USER role; only an admin can grant ADMIN. */
    @Transactional
    public User register(String username, String email, String rawPassword) {
        String normalisedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByUsername(username)) {
            throw AuthException.conflict("Username is already taken");
        }
        if (users.existsByEmail(normalisedEmail)) {
            throw AuthException.conflict("Email is already registered");
        }
        Role userRole = roles.findByName(RoleName.USER).orElseThrow();
        User saved = users.save(new User(username, normalisedEmail, passwordEncoder.encode(rawPassword), Set.of(userRole)));
        meterRegistry.counter("auth_registrations_total").increment();
        return saved;
    }

    @Transactional(readOnly = true)
    public TokenPair login(String username, String rawPassword) {
        User user = users.findByUsername(username)
                .filter(User::isEnabled)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()))
                .orElseThrow(() -> {
                    meterRegistry.counter("auth_logins_total", "result", "failure").increment();
                    return AuthException.invalidCredentials();
                });
        meterRegistry.counter("auth_logins_total", "result", "success").increment();
        return issueTokens(user);
    }

    /**
     * Refresh-token rotation: the presented token is consumed and a new pair is issued.
     * A stolen token that has already been used (by the real client or the attacker) fails.
     */
    @Transactional(readOnly = true)
    public TokenPair refresh(String refreshToken) {
        long userId = refreshTokens.consume(RefreshTokens.hash(refreshToken))
                .orElseThrow(AuthException::invalidRefreshToken);
        User user = users.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(AuthException::invalidRefreshToken);
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        refreshTokens.revoke(RefreshTokens.hash(refreshToken));
    }

    private TokenPair issueTokens(User user) {
        String refresh = RefreshTokens.newToken();
        refreshTokens.save(RefreshTokens.hash(refresh), user.getId(), jwtProperties.refreshTokenTtl());
        return new TokenPair(accessTokens.issue(user), refresh, accessTokens.accessTokenTtlSeconds());
    }
}
