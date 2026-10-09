package com.comply.clients.controller;

import com.comply.api.ApiErrorBodies;
import com.comply.audit.entity.AuditAction;
import com.comply.audit.service.AuditService;
import com.comply.clients.entity.BeneficialOwner;
import com.comply.clients.entity.BeneficialOwnerRole;
import com.comply.clients.service.KycService;
import com.comply.users.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clients")
public class KycController {

  private final KycService kycService;
  private final AuditService auditService;

  public KycController(KycService kycService, AuditService auditService) {
    this.kycService = kycService;
    this.auditService = auditService;
  }

  @GetMapping("/{clientId}/beneficial-owners")
  public ResponseEntity<List<BeneficialOwner>> listOwners(
      @PathVariable UUID clientId, Authentication authentication) {
    User actor = (User) authentication.getPrincipal();
    return ResponseEntity.ok(kycService.listOwners(clientId, actor));
  }

  public record BeneficialOwnerRequest(
      @NotBlank String fullName,
      @NotNull BeneficialOwnerRole role,
      BigDecimal ownershipPercentage) {}

  @PostMapping("/{clientId}/beneficial-owners")
  @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')")
  public ResponseEntity<?> addOwner(
      @PathVariable UUID clientId,
      @Valid @RequestBody BeneficialOwnerRequest request,
      Authentication authentication) {
    try {
      BeneficialOwner owner =
          kycService.addOwner(
              clientId,
              request.fullName(),
              request.role(),
              request.ownershipPercentage(),
              (User) authentication.getPrincipal());
      audit(
          authentication,
          AuditAction.CLIENT_UPDATED,
          clientId,
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
      BeneficialOwner owner = kycService.verifyOwner(ownerId, (User) authentication.getPrincipal());
      audit(
          authentication,
          AuditAction.CLIENT_UPDATED,
          owner.getClient().getId(),
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
      var client = kycService.recalculateAndPersist(clientId, (User) authentication.getPrincipal());
      audit(
          authentication,
          AuditAction.CLIENT_UPDATED,
          clientId,
          "{\"action\":\"KYC_RECALCULATED\",\"complete\":" + client.isKycComplete() + "}");
      return ResponseEntity.ok(Map.of("clientId", clientId, "kycComplete", client.isKycComplete()));
    } catch (IllegalArgumentException exception) {
      return ResponseEntity.status(404).body(ApiErrorBodies.notFound());
    }
  }

  private void audit(
      Authentication authentication, AuditAction action, UUID resourceId, String details) {
    auditService.record(
        (User) authentication.getPrincipal(), action, "clients", resourceId, details);
  }
}
