package com.complyz.insights.controller;

import com.complyz.insights.dto.InsightsSummaryResponse;
import com.complyz.insights.service.InsightsService;
import com.complyz.users.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/insights")
public class InsightsController {

    private final InsightsService insightsService;

    public InsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<InsightsSummaryResponse> getSummary(Authentication authentication) {
        User actor = (User) authentication.getPrincipal();
        return ResponseEntity.ok(insightsService.getSummary(actor));
    }
}
