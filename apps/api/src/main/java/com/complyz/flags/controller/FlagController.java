package com.complyz.flags.controller;

import com.complyz.flags.entity.Flag;
import com.complyz.flags.entity.FlagStatus;
import com.complyz.flags.entity.FlagType;
import com.complyz.flags.service.FlagService;
import com.complyz.reporting.entity.SuspiciousActivityReport;
import com.complyz.users.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/flags")
public class FlagController {

    private final FlagService flagService;

    @Value("${complyz.institution-name:Complyz Microfinanças SARL}")
    private String institutionName;

    public FlagController(FlagService flagService) {
        this.flagService = flagService;
    }

    @GetMapping
    public ResponseEntity<List<Flag>> listFlags(
            @RequestParam(required = false) FlagStatus status,
            @RequestParam(required = false) FlagType type
    ) {
        List<Flag> flags = flagService.listFlags(status, type);
        return ResponseEntity.ok(flags);
    }

    public record ReviewFlagRequest(@NotBlank String note, FlagStatus status) {}

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')")
    public ResponseEntity<?> reviewFlag(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewFlagRequest request,
            Authentication authentication
    ) {
        User reviewer = (User) authentication.getPrincipal();
        Flag reviewed = flagService.reviewFlag(id, reviewer, request.note(), request.status());
        return ResponseEntity.ok(reviewed);
    }

    @PostMapping("/{id}/escalate-to-str")
    @PreAuthorize("hasRole('OCOS')")
    public ResponseEntity<?> escalateToStr(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        User ocos = (User) authentication.getPrincipal();
        SuspiciousActivityReport report = flagService.escalateToStr(id, ocos, institutionName);
        return ResponseEntity.ok(Map.of(
                "message", "Sinal escalado com sucesso para investigação",
                "strId", report.getId()
        ));
    }
}
