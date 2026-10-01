package com.fincomp.insights.controller;

import com.fincomp.insights.dto.InsightsSummaryResponse;
import com.fincomp.insights.service.InsightsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
public class InsightsController {

    private final InsightsService insightsService;

    public InsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<InsightsSummaryResponse> getSummary() {
        return ResponseEntity.ok(insightsService.getSummary());
    }
}
