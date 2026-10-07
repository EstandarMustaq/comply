package com.complyz.compliance.service;

import com.complyz.clients.entity.Client;
import com.complyz.clients.repository.ClientRepository;
import com.complyz.flags.entity.Flag;
import com.complyz.flags.entity.FlagStatus;
import com.complyz.flags.entity.FlagType;
import com.complyz.flags.repository.FlagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PepScreeningService {

    private static final Logger log = LoggerFactory.getLogger(PepScreeningService.class);

    private final ClientRepository clientRepository;
    private final FlagRepository flagRepository;

    @Value("${complyz.compliance.pep.max-review-interval-days:365}")
    private int maxReviewIntervalDays;

    public PepScreeningService(ClientRepository clientRepository, FlagRepository flagRepository) {
        this.clientRepository = clientRepository;
        this.flagRepository = flagRepository;
    }

    @Scheduled(cron = "0 0 2 * * ?") // Runs daily at 02:00
    @Transactional
    public int checkAndGeneratePepReviewFlags() {
        LocalDate cutoffDate = LocalDate.now().minusDays(maxReviewIntervalDays);
        List<Client> overduePeps = clientRepository
                .findByPepTrueAndPepLastReviewedAtBeforeOrPepTrueAndPepLastReviewedAtIsNull(cutoffDate);

        int count = 0;
        for (Client client : overduePeps) {
            if (client.getOrganization() == null || client.getOrganization().getId() == null) {
                log.warn("Skipping PEP review flag for client without an organization");
                continue;
            }
            boolean hasPendingFlag = flagRepository.countByClientIdAndOrganizationIdAndTypeAndStatus(
                    client.getId(), client.getOrganization().getId(), FlagType.PEP_REVIEW_DUE,
                    FlagStatus.PENDING) > 0;

            if (!hasPendingFlag) {
                String reason = String.format(
                        "The politically exposed person review is overdue by more than 12 months (last review: %s). " +
                        "Update the review and relationship assessment.",
                        client.getPepLastReviewedAt() != null ? client.getPepLastReviewedAt().toString() : "Never reviewed"
                );

                Flag flag = new Flag(client, null, FlagType.PEP_REVIEW_DUE, reason, null);
                flagRepository.save(flag);
                count++;
            }
        }

        if (count > 0) {
            log.info("Created {} overdue politically exposed person review alerts", count);
        }
        return count;
    }
}
