package com.rajkumar.gateway.auth.api;

import com.rajkumar.gateway.auth.api.AuthDtos.LoginRequest;
import com.rajkumar.gateway.auth.api.AuthDtos.MeResponse;
import com.rajkumar.gateway.auth.api.AuthDtos.RefreshRequest;
import com.rajkumar.gateway.auth.api.AuthDtos.RegisterRequest;
import com.rajkumar.gateway.auth.api.AuthDtos.TokenResponse;
import com.rajkumar.gateway.auth.api.AuthDtos.UserResponse;
import com.rajkumar.gateway.auth.domain.User;
import com.rajkumar.gateway.auth.service.AuthService;
import com.rajkumar.gateway.auth.service.TokenPair;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.username(), request.email(), request.password());
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/v1/admin/users/{id}").buildAndExpand(user.getId()).toUri())
                .body(UserResponse.from(user));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return toResponse(authService.login(request.username(), request.password()));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return toResponse(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearer-jwt")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        Number uid = jwt.getClaim("uid");
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new MeResponse(jwt.getSubject(), uid == null ? null : uid.longValue(), roles == null ? List.of() : roles);
    }

    private static TokenResponse toResponse(TokenPair pair) {
        return new TokenResponse(pair.accessToken(), pair.refreshToken(), "Bearer", pair.expiresInSeconds());
    }
}
