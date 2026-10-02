package com.complyz.sanctions.service;

import com.complyz.clients.entity.Client;
import com.complyz.clients.repository.ClientRepository;
import com.complyz.flags.entity.Flag;
import com.complyz.flags.repository.FlagRepository;
import com.complyz.sanctions.entity.SanctionsEntityType;
import com.complyz.sanctions.entity.SanctionsListEntry;
import com.complyz.sanctions.entity.SanctionsSourceList;
import com.complyz.sanctions.repository.SanctionsListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SanctionsScreeningServiceTest {

    @Mock
    private SanctionsListRepository sanctionsRepository;

    @Mock
    private FlagRepository flagRepository;

    @Mock
    private ClientRepository clientRepository;

    private SanctionsScreeningService service;

    @BeforeEach
    void setUp() {
        service = new SanctionsScreeningService(sanctionsRepository, flagRepository, clientRepository);
    }

    @Test
    void screensAllEntriesSoMisspelledNamesAreNotExcludedByInitialLikeQuery() {
        Client client = new Client("ABU YASIR HASAN", "BI", "123");
        SanctionsListEntry entry = new SanctionsListEntry(
                "ABU YASIR HASSAN",
                SanctionsEntityType.INDIVIDUAL,
                SanctionsSourceList.LISTA_DESIGNADA_NACIONAL
        );
        when(sanctionsRepository.findAll()).thenReturn(List.of(entry));
        when(flagRepository.save(any(Flag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.screenClientAgainstSanctions(client, null);

        assertTrue(result.isPresent());
        assertTrue(client.isSanctionsFrozen());
        assertNotNull(client.getSanctionsFrozenAt());
        verify(clientRepository).save(client);
    }

    @Test
    void matchesNamesWithDifferentTokenOrder() {
        Client client = new Client("HASSAN ABU YASIR", "BI", "123");
        SanctionsListEntry entry = new SanctionsListEntry(
                "ABU YASIR HASSAN",
                SanctionsEntityType.INDIVIDUAL,
                SanctionsSourceList.UN_CONSOLIDATED
        );
        when(sanctionsRepository.findAll()).thenReturn(List.of(entry));
        when(flagRepository.save(any(Flag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(service.screenClientAgainstSanctions(client, null).isPresent());
        assertTrue(client.isSanctionsFrozen());
    }

    @Test
    void doesNotFreezeWhenThereIsNoMatch() {
        Client client = new Client("Maria Silva", "BI", "123");
        SanctionsListEntry entry = new SanctionsListEntry(
                "ABU YASIR HASSAN",
                SanctionsEntityType.INDIVIDUAL,
                SanctionsSourceList.UN_CONSOLIDATED
        );
        when(sanctionsRepository.findAll()).thenReturn(List.of(entry));

        assertTrue(service.screenClientAgainstSanctions(client, null).isEmpty());
        assertFalse(client.isSanctionsFrozen());
        verifyNoInteractions(clientRepository);
    }
}
