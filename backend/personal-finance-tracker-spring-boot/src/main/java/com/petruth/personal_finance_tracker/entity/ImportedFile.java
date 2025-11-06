package com.petruth.personal_finance_tracker.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "imported_files")
public class ImportedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    // Import results
    @Column(name = "total_rows")
    private Integer totalRows = 0;

    @Column(name = "successful_imports")
    private Integer successfulImports = 0;

    @Column(name = "failed_imports")
    private Integer failedImports = 0;

    @Column(name = "duplicate_skipped")
    private Integer duplicateSkipped = 0;

    // Column mapping used
    @Column(name = "date_column")
    private String dateColumn;

    @Column(name = "amount_column")
    private String amountColumn;

    @Column(name = "description_column")
    private String descriptionColumn;

    @Column(name = "type_column")
    private String typeColumn;

    // Import metadata
    @Column(name = "import_status")
    private String importStatus = "processing"; // processing, completed, failed

    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog; // JSON array of errors

    @CreationTimestamp
    @Column(name = "imported_at", updatable = false)
    private LocalDateTime importedAt;

    // Constructors
    public ImportedFile() {
        this.importStatus = "processing";
        this.totalRows = 0;
        this.successfulImports = 0;
        this.failedImports = 0;
        this.duplicateSkipped = 0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getSuccessfulImports() {
        return successfulImports;
    }

    public void setSuccessfulImports(Integer successfulImports) {
        this.successfulImports = successfulImports;
    }

    public Integer getFailedImports() {
        return failedImports;
    }

    public void setFailedImports(Integer failedImports) {
        this.failedImports = failedImports;
    }

    public Integer getDuplicateSkipped() {
        return duplicateSkipped;
    }

    public void setDuplicateSkipped(Integer duplicateSkipped) {
        this.duplicateSkipped = duplicateSkipped;
    }

    public String getDateColumn() {
        return dateColumn;
    }

    public void setDateColumn(String dateColumn) {
        this.dateColumn = dateColumn;
    }

    public String getAmountColumn() {
        return amountColumn;
    }

    public void setAmountColumn(String amountColumn) {
        this.amountColumn = amountColumn;
    }

    public String getDescriptionColumn() {
        return descriptionColumn;
    }

    public void setDescriptionColumn(String descriptionColumn) {
        this.descriptionColumn = descriptionColumn;
    }

    public String getTypeColumn() {
        return typeColumn;
    }

    public void setTypeColumn(String typeColumn) {
        this.typeColumn = typeColumn;
    }

    public String getImportStatus() {
        return importStatus;
    }

    public void setImportStatus(String importStatus) {
        this.importStatus = importStatus;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(String errorLog) {
        this.errorLog = errorLog;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(LocalDateTime importedAt) {
        this.importedAt = importedAt;
    }
}