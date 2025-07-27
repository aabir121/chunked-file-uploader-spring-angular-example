package com.example.largeupload.exception;

import lombok.Getter;

/**
 * Custom exception for file upload related errors
 */
@Getter
public class FileUploadException extends RuntimeException {

    private static final String DEFAULT_ERROR_CODE = "UPLOAD_ERROR";

    private final String fileId;
    private final Integer chunkNumber;
    private final String errorCode;
    
    public FileUploadException(String message) {
        super(message);
        this.fileId = null;
        this.chunkNumber = null;
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
        this.fileId = null;
        this.chunkNumber = null;
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    public FileUploadException(String message, String fileId, Integer chunkNumber) {
        super(message);
        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    public FileUploadException(String message, String fileId, Integer chunkNumber, Throwable cause) {
        super(message, cause);
        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.errorCode = DEFAULT_ERROR_CODE;
    }
    
    public FileUploadException(String message, String fileId, Integer chunkNumber, String errorCode, Throwable cause) {
        super(message, cause);
        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.errorCode = errorCode;
    }
    

}
