package com.example.largeupload.exception;

import lombok.Getter;

/**
 * Custom exception for file storage related errors
 */
@Getter
public class FileStorageException extends RuntimeException {

    private static final String DEFAULT_ERROR_CODE = "STORAGE_ERROR";

    private final String fileId;
    private final String operation;
    private final String errorCode;
    
    public FileStorageException(String message) {
        super(message);
        this.fileId = null;
        this.operation = null;
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
        this.fileId = null;
        this.operation = null;
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    public FileStorageException(String message, String fileId, String operation) {
        super(message);
        this.fileId = fileId;
        this.operation = operation;
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    public FileStorageException(String message, String fileId, String operation, Throwable cause) {
        super(message, cause);
        this.fileId = fileId;
        this.operation = operation;
        this.errorCode = DEFAULT_ERROR_CODE;
    }
    
    public FileStorageException(String message, String fileId, String operation, String errorCode, Throwable cause) {
        super(message, cause);
        this.fileId = fileId;
        this.operation = operation;
        this.errorCode = errorCode;
    }
    

}
