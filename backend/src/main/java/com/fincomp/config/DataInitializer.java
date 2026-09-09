package com.fincomp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincomp.sanctions.entity.SanctionsEntityType;
import com.fincomp.sanctions.entity.SanctionsListEntry;
import com.fincomp.sanctions.entity.SanctionsSourceList;
import com.fincomp.sanctions.repository.SanctionsListRepository;
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

    public DataInitializer(
            SanctionsListRepository sanctionsListRepository,
            ObjectMapper objectMapper
    ) {
        this.sanctionsListRepository = sanctionsListRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        if (sanctionsListRepository.count() == 0) {
            try (var is = getClass().getResourceAsStream("/sanctions_nacional_seed.json")) {
                if (is == null) {
                    throw new IllegalStateException("Seed obrigatório de sanções não encontrado");
                }

                List<Map<String, String>> entries = objectMapper.readValue(is, List.class);
                if (entries.isEmpty()) {
                    throw new IllegalStateException("Seed obrigatório de sanções está vazio");
                }

                for (Map<String, String> entry : entries) {
                    validateEntry(entry);
                    SanctionsListEntry sle = new SanctionsListEntry(
                            entry.get("fullName"),
                            SanctionsEntityType.valueOf(entry.get("entityType")),
                            SanctionsSourceList.valueOf(entry.get("sourceList")),
                            entry.get("rawReference")
                    );
                    sanctionsListRepository.save(sle);
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
