package com.attendease.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_data_exports")
public class AdminDataExport {

    public static final String LATEST_EXPORT_ID = "latest";

    @Id
    @Column(length = 20)
    private String id = LATEST_EXPORT_ID;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 150)
    private String contentType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contentBase64;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void updateGeneratedAt() {
        generatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void refreshGeneratedAt() {
        generatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentBase64() {
        return contentBase64;
    }

    public void setContentBase64(String contentBase64) {
        this.contentBase64 = contentBase64;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}
