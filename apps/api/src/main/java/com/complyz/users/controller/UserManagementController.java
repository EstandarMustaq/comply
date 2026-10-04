package com.complyz.users.controller;

import com.complyz.api.ApiErrorBodies;
import com.complyz.api.ApiErrorEnvelope;
import com.complyz.audit.entity.AuditAction;
import com.complyz.audit.service.AuditService;
import com.complyz.users.entity.Role;
import com.complyz.users.entity.User;
import com.complyz.users.repository.UserRepository;
import com.complyz.tenancy.service.TenantAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/settings/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final TenantAccessService tenantAccessService;

    public UserManagementController(UserRepository userRepository, AuditService auditService,
                                    TenantAccessService tenantAccessService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listUsers(Authentication authentication) {
        User actor = (User) authentication.getPrincipal();
        UUID organizationId = tenantAccessService.organizationId(actor);
        List<Map<String, Object>> users = userRepository.findAllByOrganizationId(organizationId).stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "fullName", u.getFullName(),
                        "email", u.getEmail(),
                        "role", u.getRole(),
                        "mfaEnabled", u.isMfaEnabled(),
                        "createdAt", u.getCreatedAt(),
                        "lastLoginAt", u.getLastLoginAt() != null ? u.getLastLoginAt() : ""
                )).toList();

        return ResponseEntity.ok(users);
    }

    public record CreateUserRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotNull Role role
    ) {}

    @PostMapping
    @ApiResponse(
            responseCode = "501",
            description = "A criação directa de utilizadores não está disponível.",
            content = @Content(schema = @Schema(implementation = ApiErrorEnvelope.class))
    )
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "A criação directa de utilizadores não está disponível."));
    }

    @PatchMapping("/{id}/reset-mfa")
    public ResponseEntity<?> resetMfa(@PathVariable UUID id, Authentication authentication) {
        User actor = (User) authentication.getPrincipal();
        User user = userRepository.findByIdAndOrganizationId(id, tenantAccessService.organizationId(actor))
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(ApiErrorBodies.notFound());
        }

        user.setMfaEnabled(false);
        user.setMfaSecretEncrypted(null);
        user.setMfaBackupCodesHash(null);
        userRepository.save(user);

        auditService.record(actor, AuditAction.USER_UPDATED, "users", user.getId(), "{\"action\": \"RESET_MFA\"}");

        return ResponseEntity.ok(Map.of("message", "MFA redefinido com sucesso para " + user.getEmail()));
    }
}
