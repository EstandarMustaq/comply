package com.fincomp.sanctions.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sanctions_list_entries")
public class SanctionsListEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private SanctionsEntityType entityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_list", nullable = false)
    private SanctionsSourceList sourceList;

    @Column(name = "raw_reference")
    private String rawReference;

    @Column(name = "imported_at", nullable = false, updatable = false)
    private Instant importedAt = Instant.now();

    public SanctionsListEntry() {
    }

    public SanctionsListEntry(String fullName, SanctionsEntityType entityType, SanctionsSourceList sourceList, String rawReference) {
        this.fullName = fullName;
        this.entityType = entityType;
        this.sourceList = sourceList;
        this.rawReference = rawReference;
        this.importedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public SanctionsEntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(SanctionsEntityType entityType) {
        this.entityType = entityType;
    }

    public SanctionsSourceList getSourceList() {
        return sourceList;
    }

    public void setSourceList(SanctionsSourceList sourceList) {
        this.sourceList = sourceList;
    }

    public String getRawReference() {
        return rawReference;
    }

    public void setRawReference(String rawReference) {
        this.rawReference = rawReference;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(Instant importedAt) {
        this.importedAt = importedAt;
    }
}
