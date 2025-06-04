package com.airtribe.SimpleCloudStorage.exceptionHandler;

public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileStorageException(Integer fileId, String operation, Throwable cause) {
        super("Error during " + operation + " for file ID: " + fileId, cause);
    }
}
