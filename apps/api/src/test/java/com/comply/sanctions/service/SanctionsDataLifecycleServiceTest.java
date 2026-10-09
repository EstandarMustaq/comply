package com.comply.sanctions.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.comply.sanctions.entity.*;
import com.comply.sanctions.repository.SanctionsListRepository;
import com.comply.sanctions.repository.SanctionsListVersionRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SanctionsDataLifecycleServiceTest {
  @Test
  void registersWithSha256AndLinksEntriesToManifest() {
    SanctionsListRepository entries = mock(SanctionsListRepository.class);
    SanctionsListVersionRepository versions = mock(SanctionsListVersionRepository.class);
    when(versions.findByVersion("v2")).thenReturn(Optional.empty());
    when(versions.save(any(SanctionsListVersion.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    SanctionsListEntry entry =
        new SanctionsListEntry(
            "Maria Silva", SanctionsEntityType.INDIVIDUAL, SanctionsSourceList.UN_CONSOLIDATED);
    SanctionsDataLifecycleService service = new SanctionsDataLifecycleService(entries, versions);

    SanctionsListVersion result =
        service.registerVersion(
            "v2",
            SanctionsSourceList.UN_CONSOLIDATED,
            "https://example.test/un.json",
            "payload".getBytes(StandardCharsets.UTF_8),
            List.of(entry));

    assertEquals(
        "239f59ed55e737c77147cf55ad0c1b030b6d7ee748a7426952f9b852d5a935e5",
        result.getChecksum(),
        "checksum must be deterministic");
    assertSame(result, entry.getVersion());
    verify(entries).save(entry);
  }

  @Test
  void activationClosesPreviousActiveVersionForSameSource() {
    SanctionsListRepository entries = mock(SanctionsListRepository.class);
    SanctionsListVersionRepository versions = mock(SanctionsListVersionRepository.class);
    SanctionsListVersion oldVersion =
        new SanctionsListVersion(
            "v1",
            SanctionsSourceList.UN_CONSOLIDATED,
            "old",
            "source",
            Instant.now().minusSeconds(10));
    SanctionsListVersion newVersion =
        new SanctionsListVersion(
            "v2", SanctionsSourceList.UN_CONSOLIDATED, "new", "source", Instant.now());
    oldVersion.setStatus(SanctionsVersionStatus.ACTIVE);
    when(versions.findByVersion("v2")).thenReturn(Optional.of(newVersion));
    when(versions.findByStatus(SanctionsVersionStatus.ACTIVE)).thenReturn(List.of(oldVersion));
    when(versions.save(any(SanctionsListVersion.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    SanctionsDataLifecycleService service = new SanctionsDataLifecycleService(entries, versions);
    service.activate("v2");

    assertEquals(SanctionsVersionStatus.INACTIVE, oldVersion.getStatus());
    assertEquals(SanctionsVersionStatus.ACTIVE, newVersion.getStatus());
    assertNotNull(oldVersion.getValidUntil());
  }
}
