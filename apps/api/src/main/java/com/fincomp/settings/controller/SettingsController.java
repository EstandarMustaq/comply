package com.fincomp.settings.controller;

import com.fincomp.audit.entity.AuditAction;
import com.fincomp.audit.service.AuditService;
import com.fincomp.monitoring.entity.ThresholdRule;
import com.fincomp.monitoring.repository.ThresholdRuleRepository;
import com.fincomp.settings.entity.SystemConfig;
import com.fincomp.settings.repository.SystemConfigRepository;
import com.fincomp.users.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final ThresholdRuleRepository thresholdRuleRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final AuditService auditService;

    public SettingsController(
            ThresholdRuleRepository thresholdRuleRepository,
            SystemConfigRepository systemConfigRepository,
            AuditService auditService
    ) {
        this.thresholdRuleRepository = thresholdRuleRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.auditService = auditService;
    }

    @GetMapping("/threshold-rules")
    public ResponseEntity<List<ThresholdRule>> listThresholdRules() {
        return ResponseEntity.ok(thresholdRuleRepository.findAll());
    }

    public record UpdateThresholdRuleRequest(
            BigDecimal valueThreshold,
            Integer aggregationWindowDays,
            Boolean active
    ) {}

    @PutMapping("/threshold-rules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OCOS')")
    public ResponseEntity<?> updateThresholdRule(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateThresholdRuleRequest request,
            Authentication authentication
    ) {
        ThresholdRule rule = thresholdRuleRepository.findById(id).orElse(null);
        if (rule == null) return ResponseEntity.notFound().build();

        if (request.valueThreshold() != null) rule.setValueThreshold(request.valueThreshold());
        if (request.aggregationWindowDays() != null) rule.setAggregationWindowDays(request.aggregationWindowDays());
        if (request.active() != null) rule.setActive(request.active());

        ThresholdRule saved = thresholdRuleRepository.save(rule);

        User actor = (User) authentication.getPrincipal();
        auditService.record(actor, AuditAction.SETTINGS_UPDATED, "threshold_rules", rule.getId(),
                "{\"rule\": \"" + rule.getName() + "\", \"threshold\": " + rule.getValueThreshold() + "}");

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/system-config")
    public ResponseEntity<List<SystemConfig>> listSystemConfigs() {
        return ResponseEntity.ok(systemConfigRepository.findAll());
    }

    public record UpdateSystemConfigRequest(@NotNull String jsonValue) {}

    @PutMapping("/system-config/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSystemConfig(
            @PathVariable String key,
            @Valid @RequestBody UpdateSystemConfigRequest request,
            Authentication authentication
    ) {
        SystemConfig config = systemConfigRepository.findById(key).orElse(null);
        User actor = (User) authentication.getPrincipal();

        if (config == null) {
            config = new SystemConfig(key, request.jsonValue(), actor);
        } else {
            config.setValue(request.jsonValue());
            config.setUpdatedBy(actor);
            config.setUpdatedAt(Instant.now());
        }

        SystemConfig saved = systemConfigRepository.save(config);

        auditService.record(actor, AuditAction.SETTINGS_UPDATED, "system_config", UUID.randomUUID(),
                "{\"key\": \"" + key + "\"}");

        return ResponseEntity.ok(saved);
    }
}
