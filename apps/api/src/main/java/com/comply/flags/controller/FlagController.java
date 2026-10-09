package com.comply.flags.controller;

import com.comply.flags.entity.Flag;
import com.comply.flags.entity.FlagStatus;
import com.comply.flags.entity.FlagType;
import com.comply.flags.service.FlagService;
import com.comply.reporting.entity.SuspiciousActivityReport;
import com.comply.users.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/flags")
public class FlagController {

  private final FlagService flagService;

  @Value("${comply.institution-name:Comply Microfinanças SARL}")
  private String institutionName;

  public FlagController(FlagService flagService) {
    this.flagService = flagService;
  }

  @GetMapping
  public ResponseEntity<List<Flag>> listFlags(
      @RequestParam(required = false) FlagStatus status,
      @RequestParam(required = false) FlagType type,
      Authentication authentication) {
    User actor = (User) authentication.getPrincipal();
    List<Flag> flags = flagService.listFlags(actor, status, type);
    return ResponseEntity.ok(flags);
  }

  public record ReviewFlagRequest(@NotBlank String note, FlagStatus status) {}

  @PatchMapping("/{id}/review")
  @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')")
  public ResponseEntity<?> reviewFlag(
      @PathVariable UUID id,
      @Valid @RequestBody ReviewFlagRequest request,
      Authentication authentication) {
    User reviewer = (User) authentication.getPrincipal();
    Flag reviewed = flagService.reviewFlag(id, reviewer, request.note(), request.status());
    return ResponseEntity.ok(reviewed);
  }

  @PostMapping("/{id}/escalate-to-str")
  @PreAuthorize("hasRole('OCOS')")
  public ResponseEntity<?> escalateToStr(@PathVariable UUID id, Authentication authentication) {
    User ocos = (User) authentication.getPrincipal();
    SuspiciousActivityReport report = flagService.escalateToStr(id, ocos, institutionName);
    return ResponseEntity.ok(
        Map.of("message", "Alert escalated for investigation.", "strId", report.getId()));
  }
}
