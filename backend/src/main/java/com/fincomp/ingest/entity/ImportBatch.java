package com.fincomp.ingest.entity;

import com.fincomp.users.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_batches")
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    private ImportFormat format;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "column_mapping", columnDefinition = "jsonb")
    private String columnMapping;

    @Column(name = "total_rows", nullable = false)
    private int totalRows = 0;

    @Column(name = "error_rows", nullable = false)
    private int errorRows = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by")
    private User importedBy;

    @Column(name = "imported_at", nullable = false, updatable = false)
    private Instant importedAt = Instant.now();

    public ImportBatch() {
    }

    public ImportBatch(String fileName, ImportFormat format, User importedBy) {
        this.fileName = fileName;
        this.format = format;
        this.importedBy = importedBy;
        this.importedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public ImportFormat getFormat() {
        return format;
    }

    public void setFormat(ImportFormat format) {
        this.format = format;
    }

    public String getColumnMapping() {
        return columnMapping;
    }

    public void setColumnMapping(String columnMapping) {
        this.columnMapping = columnMapping;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getErrorRows() {
        return errorRows;
    }

    public void setErrorRows(int errorRows) {
        this.errorRows = errorRows;
    }

    public User getImportedBy() {
        return importedBy;
    }

    public void setImportedBy(User importedBy) {
        this.importedBy = importedBy;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(Instant importedAt) {
        this.importedAt = importedAt;
    }
}
