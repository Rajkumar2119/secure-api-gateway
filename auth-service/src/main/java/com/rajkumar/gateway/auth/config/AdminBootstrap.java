package com.rajkumar.gateway.auth.config;

import com.rajkumar.gateway.auth.domain.Role;
import com.rajkumar.gateway.auth.domain.RoleName;
import com.rajkumar.gateway.auth.domain.RoleRepository;
import com.rajkumar.gateway.auth.domain.User;
import com.rajkumar.gateway.auth.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Creates the first administrator on start-up when {@code app.bootstrap-admin.password}
 * is set (docker-compose sets it for the demo). Does nothing if the user already exists.
 */
@Component
@ConditionalOnProperty(prefix = "app.bootstrap-admin", name = "password")
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    AdminBootstrap(UserRepository users, RoleRepository roles, PasswordEncoder passwordEncoder,
                   @Value("${app.bootstrap-admin.username:admin}") String username,
                   @Value("${app.bootstrap-admin.password}") String password) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByUsername(username)) {
            return;
        }
        Role admin = roles.findByName(RoleName.ADMIN).orElseThrow();
        Role user = roles.findByName(RoleName.USER).orElseThrow();
        users.save(new User(username, username + "@example.com", passwordEncoder.encode(password), Set.of(admin, user)));
        log.info("Created bootstrap admin user '{}'", username);
    }
}
