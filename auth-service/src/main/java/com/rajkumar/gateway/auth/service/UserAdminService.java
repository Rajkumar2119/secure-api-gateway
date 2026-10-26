package com.rajkumar.gateway.auth.service;

import com.rajkumar.gateway.auth.domain.Role;
import com.rajkumar.gateway.auth.domain.RoleName;
import com.rajkumar.gateway.auth.domain.RoleRepository;
import com.rajkumar.gateway.auth.domain.User;
import com.rajkumar.gateway.auth.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/** Admin-only operations. {@code @PreAuthorize} is a second check behind the URL rules. */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminService {

    private final UserRepository users;
    private final RoleRepository roles;

    public UserAdminService(UserRepository users, RoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public Page<User> list(Pageable pageable) {
        return users.findAll(pageable);
    }

    @Transactional
    public User replaceRoles(long userId, Set<RoleName> roleNames) {
        if (roleNames.isEmpty()) {
            throw AuthException.badRequest("A user needs at least one role");
        }
        User user = users.findById(userId).orElseThrow(() -> AuthException.notFound("User " + userId + " not found"));
        Set<Role> newRoles = new HashSet<>(roles.findByNameIn(roleNames));
        user.replaceRoles(newRoles);
        return user;
    }

    @Transactional
    public User setEnabled(long userId, boolean enabled) {
        User user = users.findById(userId).orElseThrow(() -> AuthException.notFound("User " + userId + " not found"));
        user.setEnabled(enabled);
        return user;
    }
}
