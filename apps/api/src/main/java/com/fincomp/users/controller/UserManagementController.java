package com.fincomp.users.controller;

import com.fincomp.audit.entity.AuditAction;
import com.fincomp.audit.service.AuditService;
import com.fincomp.users.entity.Role;
import com.fincomp.users.entity.User;
import com.fincomp.users.repository.UserRepository;
import com.fincomp.tenancy.service.TenantAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/settings/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final TenantAccessService tenantAccessService;

    public UserManagementController(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditService auditService,
                                    TenantAccessService tenantAccessService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listUsers(Authentication authentication) {
        User actor = (User) authentication.getPrincipal();
        UUID organizationId = tenantAccessService.organizationId(actor);
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .filter(u -> u.getOrganization() != null && organizationId.equals(u.getOrganization().getId()))
                .map(u -> Map.<String, Object>of(
                "id", u.getId(),
                "fullName", u.getFullName(),
                "email", u.getEmail(),
                "role", u.getRole(),
                "mfaEnabled", u.isMfaEnabled(),
                "createdAt", u.getCreatedAt(),
                "lastLoginAt", u.getLastLoginAt() != null ? u.getLastLoginAt() : ""
        )).collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    public record CreateUserRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotNull Role role
    ) {}

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request, Authentication authentication) {
        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email já cadastrado no sistema"));
        }

        User newUser = new User(
                request.fullName(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.role()
        );
        newUser.setOrganization(tenantAccessService.requireOrganization((User) authentication.getPrincipal()));

        User saved = userRepository.save(newUser);

        User actor = (User) authentication.getPrincipal();
        auditService.record(actor, AuditAction.USER_CREATED, "users", saved.getId(),
                "{\"email\": \"" + saved.getEmail() + "\", \"role\": \"" + saved.getRole() + "\"}");

        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "fullName", saved.getFullName(),
                "email", saved.getEmail(),
                "role", saved.getRole(),
                "mfaEnabled", saved.isMfaEnabled()
        ));
    }

    @PatchMapping("/{id}/reset-mfa")
    public ResponseEntity<?> resetMfa(@PathVariable UUID id, Authentication authentication) {
        User actor = (User) authentication.getPrincipal();
        User user = userRepository.findById(id).filter(u -> u.getOrganization() != null
                && u.getOrganization().getId().equals(tenantAccessService.organizationId(actor))).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setMfaEnabled(false);
        user.setMfaSecretEncrypted(null);
        user.setMfaBackupCodesHash(null);
        userRepository.save(user);

        auditService.record(actor, AuditAction.USER_UPDATED, "users", user.getId(), "{\"action\": \"RESET_MFA\"}");

        return ResponseEntity.ok(Map.of("message", "MFA redefinido com sucesso para " + user.getEmail()));
    }
}
