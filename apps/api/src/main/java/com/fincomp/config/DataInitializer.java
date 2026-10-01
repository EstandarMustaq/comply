package com.fincomp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincomp.sanctions.entity.SanctionsEntityType;
import com.fincomp.sanctions.entity.SanctionsListEntry;
import com.fincomp.sanctions.entity.SanctionsSourceList;
import com.fincomp.sanctions.repository.SanctionsListRepository;
import com.fincomp.sanctions.service.SanctionsDataLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SanctionsListRepository sanctionsListRepository;
    private final ObjectMapper objectMapper;
    private final SanctionsDataLifecycleService lifecycleService;

    public DataInitializer(
            SanctionsListRepository sanctionsListRepository,
            ObjectMapper objectMapper,
            SanctionsDataLifecycleService lifecycleService
    ) {
        this.sanctionsListRepository = sanctionsListRepository;
        this.objectMapper = objectMapper;
        this.lifecycleService = lifecycleService;
    }

    @Override
    public void run(String... args) {
        if (sanctionsListRepository.count() == 0) {
            try (var is = getClass().getResourceAsStream("/sanctions_nacional_seed.json")) {
                if (is == null) {
                    throw new IllegalStateException("Seed obrigatório de sanções não encontrado");
                }

                byte[] seedContent = is.readAllBytes();
                List<Map<String, String>> entries = objectMapper.readValue(seedContent, List.class);
                if (entries.isEmpty()) {
                    throw new IllegalStateException("Seed obrigatório de sanções está vazio");
                }

                List<SanctionsListEntry> parsedEntries = new java.util.ArrayList<>();
                for (Map<String, String> entry : entries) {
                    validateEntry(entry);
                    parsedEntries.add(new SanctionsListEntry(
                            entry.get("fullName"),
                            SanctionsEntityType.valueOf(entry.get("entityType")),
                            SanctionsSourceList.valueOf(entry.get("sourceList")),
                            entry.get("rawReference")
                    ));
                }
                for (SanctionsSourceList source : SanctionsSourceList.values()) {
                    List<SanctionsListEntry> sourceEntries = parsedEntries.stream()
                            .filter(entry -> entry.getSourceList() == source)
                            .toList();
                    if (!sourceEntries.isEmpty()) {
                        String version = "seed-2026-09-" + source.name().toLowerCase();
                        lifecycleService.registerVersion(
                                version,
                                source,
                                "classpath:/sanctions_nacional_seed.json",
                                seedContent,
                                sourceEntries
                        );
                        lifecycleService.activate(version);
                    }
                }
                log.info("Carga inicial de sanções concluída: {} registos carregados (Lista Nacional e ONU)", entries.size());
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao carregar seed obrigatório de sanções; aplicação bloqueada", e);
            }
        }
    }

    private void validateEntry(Map<String, String> entry) {
        if (entry == null
                || isBlank(entry.get("fullName"))
                || isBlank(entry.get("entityType"))
                || isBlank(entry.get("sourceList"))
                || isBlank(entry.get("rawReference"))) {
            throw new IllegalStateException("Entrada de sanções incompleta");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
