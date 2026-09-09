package com.fincomp.compliance.service;

import com.fincomp.clients.entity.Client;
import com.fincomp.clients.repository.ClientRepository;
import com.fincomp.flags.entity.Flag;
import com.fincomp.flags.entity.FlagStatus;
import com.fincomp.flags.entity.FlagType;
import com.fincomp.flags.repository.FlagRepository;
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

    @Value("${fincomp.compliance.pep.max-review-interval-days:365}")
    private int maxReviewIntervalDays;

    public PepScreeningService(ClientRepository clientRepository, FlagRepository flagRepository) {
        this.clientRepository = clientRepository;
        this.flagRepository = flagRepository;
    }

    @Scheduled(cron = "0 0 2 * * ?") // Todos os dias às 02:00
    @Transactional
    public int checkAndGeneratePepReviewFlags() {
        LocalDate cutoffDate = LocalDate.now().minusDays(maxReviewIntervalDays);
        List<Client> overduePeps = clientRepository
                .findByPepTrueAndPepLastReviewedAtBeforeOrPepTrueAndPepLastReviewedAtIsNull(cutoffDate);

        int count = 0;
        for (Client client : overduePeps) {
            boolean hasPendingFlag = flagRepository.existsByClientIdAndTypeAndStatus(
                    client.getId(), FlagType.PEP_REVIEW_DUE, FlagStatus.PENDING
            );

            if (!hasPendingFlag) {
                String reason = String.format(
                        "Revisão periódica de PEP vencida há mais de 12 meses (última revisão: %s). " +
                        "Obrigatória reavaliação contínua da relação de negócio nos termos dos Artigos 65 e 66 do Aviso 10/GBM/2024.",
                        client.getPepLastReviewedAt() != null ? client.getPepLastReviewedAt().toString() : "Nunca realizada"
                );

                Flag flag = new Flag(client, null, FlagType.PEP_REVIEW_DUE, reason, null);
                flagRepository.save(flag);
                count++;
            }
        }

        if (count > 0) {
            log.info("Geradas {} flags de revisão de PEP pendentes de regularização", count);
        }
        return count;
    }
}
