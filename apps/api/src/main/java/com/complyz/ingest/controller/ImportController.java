package com.complyz.ingest.controller;

import com.complyz.api.ApiErrorBodies;
import com.complyz.ingest.entity.ImportBatch;
import com.complyz.ingest.repository.ImportBatchRepository;
import com.complyz.ingest.service.ImportService;
import com.complyz.users.entity.User;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/import-batches")
public class ImportController {

    private static final Logger logger = LoggerFactory.getLogger(ImportController.class);
    private final ImportService importService;
    private final ImportBatchRepository importBatchRepository;

    public ImportController(ImportService importService, ImportBatchRepository importBatchRepository) {
        this.importService = importService;
        this.importBatchRepository = importBatchRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'OCOS')")
    public ResponseEntity<?> uploadBatch(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            ImportService.UploadPreviewResponse preview = importService.createUploadBatch(file, user);
            return ResponseEntity.ok(preview);
        } catch (Exception e) {
            logger.warn("Could not prepare uploaded import file: {}", e.getClass().getSimpleName());
            return ResponseEntity.badRequest().body(Map.of("message", "Não foi possível preparar o ficheiro. Verifique o formato e os dados."));
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
            return ResponseEntity.badRequest().body(Map.of("message", "Não foi possível processar a importação. Reveja os dados e tente novamente."));
        }
    }

    @GetMapping
    public ResponseEntity<List<ImportBatch>> listBatches() {
        return ResponseEntity.ok(importBatchRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBatch(@PathVariable UUID id) {
        return importBatchRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(ApiErrorBodies.notFound()));
    }
}
