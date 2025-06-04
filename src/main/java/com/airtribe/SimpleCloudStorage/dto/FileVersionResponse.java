package com.airtribe.SimpleCloudStorage.dto;

import java.time.Instant;

public class FileVersionResponse {
    private int version;
    private Instant createdAt;

    public FileVersionResponse(int version, Instant createdAt) {
        this.version = version;
        this.createdAt = createdAt;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
