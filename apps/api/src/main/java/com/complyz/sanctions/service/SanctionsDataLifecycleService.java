package com.complyz.sanctions.service;

import com.complyz.sanctions.entity.*;
import com.complyz.sanctions.repository.SanctionsListRepository;
import com.complyz.sanctions.repository.SanctionsListVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
public class SanctionsDataLifecycleService {
    private final SanctionsListRepository entryRepository;
    private final SanctionsListVersionRepository versionRepository;

    public SanctionsDataLifecycleService(
            SanctionsListRepository entryRepository,
            SanctionsListVersionRepository versionRepository
    ) {
        this.entryRepository = entryRepository;
        this.versionRepository = versionRepository;
    }

    public String checksum(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }

    @Transactional
    public SanctionsListVersion registerVersion(
            String version,
            SanctionsSourceList sourceList,
            String sourceUri,
            byte[] content,
            List<SanctionsListEntry> entries
    ) {
        if (version == null || version.isBlank() || sourceUri == null || sourceUri.isBlank()
                || content == null || content.length == 0 || entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("Versão, origem, conteúdo e entradas são obrigatórios");
        }
        if (versionRepository.findByVersion(version).isPresent()) {
            throw new IllegalArgumentException("Versão de sanções já registada: " + version);
        }
        String digest = checksum(content);
        SanctionsListVersion manifest = versionRepository.save(
                new SanctionsListVersion(version, sourceList, digest, sourceUri, Instant.now()));
        for (SanctionsListEntry entry : entries) {
            if (entry.getFullName() == null || entry.getFullName().isBlank()
                    || entry.getSourceList() != sourceList) {
                throw new IllegalArgumentException("Entrada de sanções inválida para a versão");
            }
            entry.setVersion(manifest);
            entryRepository.save(entry);
        }
        return manifest;
    }

    @Transactional
    public SanctionsListVersion activate(String version) {
        SanctionsListVersion target = versionRepository.findByVersion(version)
                .orElseThrow(() -> new IllegalArgumentException("Versão de sanções não encontrada: " + version));
        for (SanctionsListVersion active : versionRepository.findByStatus(SanctionsVersionStatus.ACTIVE)) {
            if (active.getSourceList() == target.getSourceList() && active != target) {
                active.setStatus(SanctionsVersionStatus.INACTIVE);
                active.setValidUntil(Instant.now());
                versionRepository.save(active);
            }
        }
        target.setStatus(SanctionsVersionStatus.ACTIVE);
        return versionRepository.save(target);
    }

    @Transactional
    public SanctionsListVersion rollback(String version) {
        SanctionsListVersion target = versionRepository.findByVersion(version)
                .orElseThrow(() -> new IllegalArgumentException("Versão de sanções não encontrada: " + version));
        target.setStatus(SanctionsVersionStatus.ROLLED_BACK);
        target.setValidUntil(Instant.now());
        return versionRepository.save(target);
    }
}
