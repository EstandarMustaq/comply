package com.fincomp.clients.controller;

import com.fincomp.audit.entity.AuditAction;
import com.fincomp.audit.service.AuditService;
import com.fincomp.clients.entity.BeneficialOwner;
import com.fincomp.clients.entity.BeneficialOwnerRole;
import com.fincomp.clients.repository.BeneficialOwnerRepository;
import com.fincomp.clients.service.KycService;
import com.fincomp.users.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
public class KycController {

    private final KycService kycService;
    private final BeneficialOwnerRepository beneficialOwnerRepository;
    private final AuditService auditService;

    public KycController(KycService kycService, BeneficialOwnerRepository beneficialOwnerRepository,
                         AuditService auditService) {
        this.kycService = kycService;
        this.beneficialOwnerRepository = beneficialOwnerRepository;
        this.auditService = auditService;
    }

    @GetMapping("/{clientId}/beneficial-owners")
    public ResponseEntity<List<BeneficialOwner>> listOwners(@PathVariable UUID clientId) {
        return ResponseEntity.ok(beneficialOwnerRepository.findByClientId(clientId));
    }

    public record BeneficialOwnerRequest(
            @NotBlank String fullName,
            @NotNull BeneficialOwnerRole role,
            BigDecimal ownershipPercentage
    ) {}

    @PostMapping("/{clientId}/beneficial-owners")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')")
    public ResponseEntity<?> addOwner(@PathVariable UUID clientId,
                                      @Valid @RequestBody BeneficialOwnerRequest request,
                                      Authentication authentication) {
        try {
            BeneficialOwner owner = kycService.addOwner(
                    clientId, request.fullName(), request.role(), request.ownershipPercentage());
            audit(authentication, AuditAction.CLIENT_UPDATED, clientId,
                    "{\"action\":\"BENEFICIAL_OWNER_ADDED\"}");
            return ResponseEntity.ok(owner);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/beneficial-owners/{ownerId}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')")
    public ResponseEntity<?> verifyOwner(@PathVariable UUID ownerId, Authentication authentication) {
        try {
            BeneficialOwner owner = kycService.verifyOwner(ownerId);
            audit(authentication, AuditAction.CLIENT_UPDATED, owner.getClient().getId(),
                    "{\"action\":\"BENEFICIAL_OWNER_VERIFIED\"}");
            return ResponseEntity.ok(owner);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/{clientId}/kyc/recalculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')")
    public ResponseEntity<?> recalculate(@PathVariable UUID clientId, Authentication authentication) {
        try {
            var client = kycService.recalculateAndPersist(clientId);
            audit(authentication, AuditAction.CLIENT_UPDATED, clientId,
                    "{\"action\":\"KYC_RECALCULATED\",\"complete\":" + client.isKycComplete() + "}");
            return ResponseEntity.ok(Map.of("clientId", clientId, "kycComplete", client.isKycComplete()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    private void audit(Authentication authentication, AuditAction action, UUID resourceId, String details) {
        auditService.record((User) authentication.getPrincipal(), action, "clients", resourceId, details);
    }
}
