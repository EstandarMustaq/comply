package com.complyz.ingest.controller;

import com.complyz.api.ApiErrorBodies;
import com.complyz.ingest.entity.ImportBatch;
import com.complyz.ingest.service.ImportService;
import com.complyz.users.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/import-batches")
public class ImportController {

    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);
    private final ImportService importService;
    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')")
    public ResponseEntity<?> uploadBatch(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            ImportService.UploadPreviewResponse preview = importService.createUploadBatch(file, user);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            logger.warn("Could not prepare uploaded import file: {}", e.getClass().getSimpleName());
            return ResponseEntity.badRequest().body(Map.of("message", "We couldn't prepare this file. Check its format and data."));
        }
    }

    @PostMapping("/{id}/confirm-mapping")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')")
    public ResponseEntity<?> confirmMapping(
            @PathVariable UUID id,
            @RequestBody Map<String, String> mapping,
            Authentication authentication
    ) {
        try {
            User user = (User) authentication.getPrincipal();
            Map<String, Object> result = importService.confirmAndProcess(id, mapping, user);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.warn("Could not process confirmed import mapping: {}", e.getClass().getSimpleName());
            return ResponseEntity.badRequest().body(Map.of("message", "We couldn't process this import. Review the data and try again."));
        }
    }

    @GetMapping
    public ResponseEntity<List<ImportBatch>> listBatches(Authentication authentication) {
        User actor = (User) authentication.getPrincipal();
        return ResponseEntity.ok(importService.listBatches(actor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBatch(@PathVariable UUID id, Authentication authentication) {
        User actor = (User) authentication.getPrincipal();
        return importService.findBatch(id, actor)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(ApiErrorBodies.notFound()));
    }
}
