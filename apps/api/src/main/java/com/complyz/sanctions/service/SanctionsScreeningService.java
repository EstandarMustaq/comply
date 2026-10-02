package com.complyz.sanctions.service;

import com.complyz.clients.entity.Client;
import com.complyz.clients.entity.RiskRating;
import com.complyz.clients.repository.ClientRepository;
import com.complyz.flags.entity.Flag;
import com.complyz.flags.entity.FlagType;
import com.complyz.flags.repository.FlagRepository;
import com.complyz.monitoring.entity.Transaction;
import com.complyz.sanctions.entity.SanctionsListEntry;
import com.complyz.sanctions.entity.SanctionsVersionStatus;
import com.complyz.sanctions.repository.SanctionsListRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SanctionsScreeningService {

    private final SanctionsListRepository sanctionsRepository;
    private final FlagRepository flagRepository;
    private final ClientRepository clientRepository;

    @Value("${complyz.compliance.sanctions.sla-hours:24}")
    private int slaHours;

    public SanctionsScreeningService(
            SanctionsListRepository sanctionsRepository,
            FlagRepository flagRepository,
            ClientRepository clientRepository
    ) {
        this.sanctionsRepository = sanctionsRepository;
        this.flagRepository = flagRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional
    public Optional<Flag> screenClientAgainstSanctions(Client client, Transaction triggeringTx) {
        if (client == null || client.getFullName() == null) return Optional.empty();

        String normalizedClientName = normalize(client.getFullName());
        Instant now = Instant.now();
        List<SanctionsListEntry> candidates = sanctionsRepository.findAll().stream()
                .filter(entry -> entry.getVersion() == null
                        || (entry.getVersion().getStatus() == SanctionsVersionStatus.ACTIVE
                        && !entry.getVersion().getValidFrom().isAfter(now)
                        && (entry.getVersion().getValidUntil() == null
                        || entry.getVersion().getValidUntil().isAfter(now))))
                .toList();

        for (SanctionsListEntry entry : candidates) {
            String normalizedSanctionName = normalize(entry.getFullName());
            if (isFuzzyMatch(normalizedClientName, normalizedSanctionName)) {
                Instant deadline = Instant.now().plus(slaHours, ChronoUnit.HOURS);
                String reason = String.format(
                        "Correspondência em lista de triagem: '%s'. Revisão prioritária em até %d horas.",
                        entry.getFullName(), slaHours
                );

                Flag flag = new Flag(client, triggeringTx, FlagType.SANCTIONS_MATCH, reason, deadline);
                Flag savedFlag = flagRepository.save(flag);

                // Freeze is persisted before the transaction leaves this compliance boundary.
                client.setRiskRating(RiskRating.HIGH);
                client.setRiskRatingUpdatedAt(LocalDate.now());
                client.setSanctionsFrozen(true);
                client.setSanctionsFrozenAt(Instant.now());
                client.setSanctionsFreezeReason(reason);
                clientRepository.save(client);

                return Optional.of(savedFlag);
            }
        }

        return Optional.empty();
    }

    private boolean isFuzzyMatch(String s1, String s2) {
        if (s1.equals(s2)) return true;
        if (s1.contains(s2) || s2.contains(s1)) return true;
        if (calculateSimilarity(s1, s2) >= 0.85) return true;

        String sortedTokens1 = sortedTokens(s1);
        String sortedTokens2 = sortedTokens(s2);
        return sortedTokens1.equals(sortedTokens2) || calculateSimilarity(sortedTokens1, sortedTokens2) >= 0.85;
    }

    private double calculateSimilarity(String s1, String s2) {
        int longer = Math.max(s1.length(), s2.length());
        if (longer == 0) return 1.0;
        int editDist = levenshteinDistance(s1, s2);
        return (longer - editDist) / (double) longer;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    }
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0) costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

    private String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String sortedTokens(String value) {
        return Arrays.stream(value.split(" "))
                .filter(token -> !token.isBlank())
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(" "));
    }
}
