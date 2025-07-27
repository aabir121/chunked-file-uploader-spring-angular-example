package com.example.largeupload.exception;

/**
 * Custom exception for file storage related errors
 */
public class FileStorageException extends RuntimeException {
    
    private final String fileId;
    private final String operation;
    private final String errorCode;
    
    public FileStorageException(String message) {
        super(message);
        this.fileId = null;
        this.operation = null;
        this.errorCode = "STORAGE_ERROR";
    }
    
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
        this.fileId = null;
        this.operation = null;
        this.errorCode = "STORAGE_ERROR";
    }
    
    public FileStorageException(String message, String fileId, String operation) {
        super(message);
        this.fileId = fileId;
        this.operation = operation;
        this.errorCode = "STORAGE_ERROR";
    }
    
    public FileStorageException(String message, String fileId, String operation, Throwable cause) {
        super(message, cause);
        this.fileId = fileId;
        this.operation = operation;
        this.errorCode = "STORAGE_ERROR";
    }
    
    public FileStorageException(String message, String fileId, String operation, String errorCode, Throwable cause) {
        super(message, cause);
        this.fileId = fileId;
        this.operation = operation;
        this.errorCode = errorCode;
    }
    
    public String getFileId() {
        return fileId;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
