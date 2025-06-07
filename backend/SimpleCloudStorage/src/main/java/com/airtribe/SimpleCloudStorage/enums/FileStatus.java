package com.airtribe.SimpleCloudStorage.enums;

import lombok.Getter;

@Getter
public enum FileStatus {
    UPLOADING("Uploading"),           // File is being uploaded
    AVAILABLE("Available"),           // File is ready for access
    PROCESSING("Processing"),          // File is being processed (e.g., virus scan)
    QUARANTINED("Quarantined"),       // File flagged as suspicious
    DELETED("Deleted"),               // Soft-deleted file
    ARCHIVED("Archived"),             // File moved to cold storage
    ERROR("Error");                   // File processing failed

    private final String displayName;

    FileStatus(String displayName) {
        this.displayName = displayName;
    }

}