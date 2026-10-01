package com.fincomp.ingest.controller;

import com.fincomp.ingest.entity.ImportBatch;
import com.fincomp.ingest.repository.ImportBatchRepository;
import com.fincomp.ingest.service.ImportService;
import com.fincomp.users.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/import-batches")
public class ImportController {

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
            return ResponseEntity.badRequest().body(Map.of("message", "Falha ao processar arquivo: " + e.getMessage()));
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
            return ResponseEntity.badRequest().body(Map.of("message", "Erro durante a ingestão: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<ImportBatch>> listBatches() {
        return ResponseEntity.ok(importBatchRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportBatch> getBatch(@PathVariable UUID id) {
        return importBatchRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
