package com.comply.compliance.service;

import com.comply.clients.entity.Client;
import com.comply.clients.repository.ClientRepository;
import com.comply.flags.entity.Flag;
import com.comply.flags.entity.FlagStatus;
import com.comply.flags.entity.FlagType;
import com.comply.flags.repository.FlagRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PepScreeningService {

  private static final Logger log = LoggerFactory.getLogger(PepScreeningService.class);

  private final ClientRepository clientRepository;
  private final FlagRepository flagRepository;

  @Value("${comply.compliance.pep.max-review-interval-days:365}")
  private int maxReviewIntervalDays;

  public PepScreeningService(ClientRepository clientRepository, FlagRepository flagRepository) {
    this.clientRepository = clientRepository;
    this.flagRepository = flagRepository;
  }

  @Scheduled(cron = "0 0 2 * * ?")
  @Transactional
  public int checkAndGeneratePepReviewFlags() {
    LocalDate cutoffDate = LocalDate.now().minusDays(maxReviewIntervalDays);
    List<Client> overduePeps =
        clientRepository.findByPepTrueAndPepLastReviewedAtBeforeOrPepTrueAndPepLastReviewedAtIsNull(
            cutoffDate);

    int count = 0;
    for (Client client : overduePeps) {
      if (client.getOrganization() == null || client.getOrganization().getId() == null) {
        log.warn("Skipping PEP review flag for client without an organization");
        continue;
      }
      boolean hasPendingFlag =
          flagRepository.countByClientIdAndOrganizationIdAndTypeAndStatus(
                  client.getId(),
                  client.getOrganization().getId(),
                  FlagType.PEP_REVIEW_DUE,
                  FlagStatus.PENDING)
              > 0;

      if (!hasPendingFlag) {
        String reason =
            String.format(
                "The politically exposed person review is overdue by more than 12 months (last review: %s). "
                    + "Update the review and relationship assessment.",
                client.getPepLastReviewedAt() != null
                    ? client.getPepLastReviewedAt().toString()
                    : "Never reviewed");

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
