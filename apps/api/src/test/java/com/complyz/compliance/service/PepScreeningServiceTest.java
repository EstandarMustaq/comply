package com.complyz.compliance.service;

import com.complyz.clients.entity.Client;
import com.complyz.clients.repository.ClientRepository;
import com.complyz.flags.entity.Flag;
import com.complyz.flags.entity.FlagStatus;
import com.complyz.flags.entity.FlagType;
import com.complyz.flags.repository.FlagRepository;
import com.complyz.tenancy.entity.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PepScreeningServiceTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private FlagRepository flagRepository;

    private PepScreeningService service;

    @BeforeEach
    void setUp() {
        service = new PepScreeningService(clientRepository, flagRepository);
        ReflectionTestUtils.setField(service, "maxReviewIntervalDays", 365);
    }

    @Test
    void includesPepClientsThatHaveNeverBeenReviewed() {
        Client client = new Client("PEP", "BI", "456");
        client.setId(UUID.randomUUID());
        client.setOrganization(organization());
        client.setPep(true);
        client.setPepLastReviewedAt(null);
        when(clientRepository.findByPepTrueAndPepLastReviewedAtBeforeOrPepTrueAndPepLastReviewedAtIsNull(
                any(LocalDate.class))).thenReturn(List.of(client));
        when(flagRepository.countByClientIdAndOrganizationIdAndTypeAndStatus(
                client.getId(), client.getOrganization().getId(), FlagType.PEP_REVIEW_DUE,
                FlagStatus.PENDING)).thenReturn(0L);
        when(flagRepository.save(any(Flag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.checkAndGeneratePepReviewFlags()).isEqualTo(1);
        verify(flagRepository).save(any(Flag.class));
    }

    @Test
    void doesNotDuplicatePendingPepReviewFlag() {
        Client client = new Client("PEP", "BI", "789");
        client.setId(UUID.randomUUID());
        client.setOrganization(organization());
        client.setPep(true);
        when(clientRepository.findByPepTrueAndPepLastReviewedAtBeforeOrPepTrueAndPepLastReviewedAtIsNull(
                any(LocalDate.class))).thenReturn(List.of(client));
        when(flagRepository.countByClientIdAndOrganizationIdAndTypeAndStatus(
                client.getId(), client.getOrganization().getId(), FlagType.PEP_REVIEW_DUE,
                FlagStatus.PENDING)).thenReturn(1L);

        assertThat(service.checkAndGeneratePepReviewFlags()).isZero();
        verify(flagRepository, never()).save(any(Flag.class));
    }

    private static Organization organization() {
        Organization organization = new Organization("test-org", "Test Organization");
        organization.setId(UUID.randomUUID());
        return organization;
    }
}
