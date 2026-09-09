package com.fincomp.sanctions.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sanctions_list_versions")
public class SanctionsListVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SanctionsSourceList sourceList;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "source_uri", nullable = false, length = 500)
    private String sourceUri;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SanctionsVersionStatus status = SanctionsVersionStatus.INACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected SanctionsListVersion() {}

    public SanctionsListVersion(
            String version,
            SanctionsSourceList sourceList,
            String checksum,
            String sourceUri,
            Instant validFrom
    ) {
        this.version = version;
        this.sourceList = sourceList;
        this.checksum = checksum;
        this.sourceUri = sourceUri;
        this.validFrom = validFrom;
    }

    public UUID getId() { return id; }
    public String getVersion() { return version; }
    public SanctionsSourceList getSourceList() { return sourceList; }
    public String getChecksum() { return checksum; }
    public String getSourceUri() { return sourceUri; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidUntil() { return validUntil; }
    public SanctionsVersionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setStatus(SanctionsVersionStatus status) { this.status = status; }
    public void setValidUntil(Instant validUntil) { this.validUntil = validUntil; }
}
