package com.airtribe.SimpleCloudStorage.dto;

import java.time.Instant;

public record ErrorResponse(
        String code,
        String message,
        String operation,
        Instant timestamp
) {
    public ErrorResponse(String code, String message) {
        this(code, message, null, Instant.now());
    }

    public ErrorResponse(String message) {
        this(null, message, null, Instant.now());
    }

    public ErrorResponse(String code, String message, String operation) {
        this(code, message, operation, Instant.now());
    }
}
