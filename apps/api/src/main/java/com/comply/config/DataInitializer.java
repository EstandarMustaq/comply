package com.comply.config;

import com.comply.sanctions.entity.SanctionsEntityType;
import com.comply.sanctions.entity.SanctionsListEntry;
import com.comply.sanctions.entity.SanctionsSourceList;
import com.comply.sanctions.repository.SanctionsListRepository;
import com.comply.sanctions.service.SanctionsDataLifecycleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
  private static final String SEED_RESOURCE = "/sanctions_national_seed.json";
  private static final String SEED_URI = "classpath:/sanctions_national_seed.json";

  private final SanctionsListRepository sanctionsListRepository;
  private final ObjectMapper objectMapper;
  private final SanctionsDataLifecycleService lifecycleService;

  public DataInitializer(
      SanctionsListRepository sanctionsListRepository,
      ObjectMapper objectMapper,
      SanctionsDataLifecycleService lifecycleService) {
    this.sanctionsListRepository = sanctionsListRepository;
    this.objectMapper = objectMapper;
    this.lifecycleService = lifecycleService;
  }

  @Override
  public void run(String... args) {
    if (sanctionsListRepository.count() == 0) {
      try (var is = getClass().getResourceAsStream(SEED_RESOURCE)) {
        if (is == null) {
          throw new IllegalStateException("Required sanctions seed was not found.");
        }

        byte[] seedContent = is.readAllBytes();
        List<SanctionsSeedEntry> entries =
            objectMapper.readValue(seedContent, new TypeReference<>() {});
        if (entries.isEmpty()) {
          throw new IllegalStateException("Required sanctions seed is empty.");
        }

        List<SanctionsListEntry> parsedEntries = new java.util.ArrayList<>();
        for (SanctionsSeedEntry entry : entries) {
          validateEntry(entry);
          parsedEntries.add(
              new SanctionsListEntry(
                  entry.fullName(),
                  SanctionsEntityType.valueOf(entry.entityType()),
                  SanctionsSourceList.valueOf(entry.sourceList())));
        }
        for (SanctionsSourceList source : SanctionsSourceList.values()) {
          List<SanctionsListEntry> sourceEntries =
              parsedEntries.stream().filter(entry -> entry.getSourceList() == source).toList();
          if (!sourceEntries.isEmpty()) {
            String version = "seed-2026-09-" + source.name().toLowerCase();
            lifecycleService.registerVersion(version, source, SEED_URI, seedContent, sourceEntries);
            lifecycleService.activate(version);
          }
        }
        log.info("Sanctions seed loaded: {} entries (national list and UN list)", entries.size());
      } catch (Exception e) {
        throw new IllegalStateException(
            "Could not load the required sanctions seed. Application startup is blocked.", e);
      }
    }
  }

  private void validateEntry(SanctionsSeedEntry entry) {
    if (entry == null
        || isBlank(entry.fullName())
        || isBlank(entry.entityType())
        || isBlank(entry.sourceList())) {
      throw new IllegalStateException("Sanctions entry is incomplete.");
    }
  }

  private record SanctionsSeedEntry(String fullName, String entityType, String sourceList) {}

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
