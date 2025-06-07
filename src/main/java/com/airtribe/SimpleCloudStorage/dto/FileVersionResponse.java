package com.airtribe.SimpleCloudStorage.dto;

import java.time.Instant;

public class FileVersionResponse {
    private String version;
    private Instant createdAt;
    private Long size;


    public FileVersionResponse(String version, Instant createdAt, Long size) {
        this.version = version;
        this.createdAt = createdAt;
        this.size = size;
    }

    public String getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant parse) {
        createdAt = parse;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
