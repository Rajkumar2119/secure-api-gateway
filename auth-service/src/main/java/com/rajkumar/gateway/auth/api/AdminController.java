package com.rajkumar.gateway.auth.api;

import com.rajkumar.gateway.auth.api.AuthDtos.EnabledRequest;
import com.rajkumar.gateway.auth.api.AuthDtos.PageResponse;
import com.rajkumar.gateway.auth.api.AuthDtos.RolesRequest;
import com.rajkumar.gateway.auth.api.AuthDtos.UserResponse;
import com.rajkumar.gateway.auth.domain.User;
import com.rajkumar.gateway.auth.service.UserAdminService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@SecurityRequirement(name = "bearer-jwt")
public class AdminController {

    private final UserAdminService adminService;

    public AdminController(UserAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public PageResponse<UserResponse> list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        Page<User> result = adminService.list(PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by("id")));
        return new PageResponse<>(result.map(UserResponse::from).getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @PutMapping("/{id}/roles")
    public UserResponse replaceRoles(@PathVariable long id, @Valid @RequestBody RolesRequest request) {
        return UserResponse.from(adminService.replaceRoles(id, request.roles()));
    }

    @PutMapping("/{id}/enabled")
    public UserResponse setEnabled(@PathVariable long id, @RequestBody EnabledRequest request) {
        return UserResponse.from(adminService.setEnabled(id, request.enabled()));
    }
}
