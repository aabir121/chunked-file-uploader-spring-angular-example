package com.example.largeupload.exception;

/**
 * Custom exception for file upload related errors
 */
public class FileUploadException extends RuntimeException {
    
    private final String fileId;
    private final Integer chunkNumber;
    private final String errorCode;
    
    public FileUploadException(String message) {
        super(message);
        this.fileId = null;
        this.chunkNumber = null;
        this.errorCode = "UPLOAD_ERROR";
    }
    
    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
        this.fileId = null;
        this.chunkNumber = null;
        this.errorCode = "UPLOAD_ERROR";
    }
    
    public FileUploadException(String message, String fileId, Integer chunkNumber) {
        super(message);
        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.errorCode = "UPLOAD_ERROR";
    }
    
    public FileUploadException(String message, String fileId, Integer chunkNumber, Throwable cause) {
        super(message, cause);
        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.errorCode = "UPLOAD_ERROR";
    }
    
    public FileUploadException(String message, String fileId, Integer chunkNumber, String errorCode, Throwable cause) {
        super(message, cause);
        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.errorCode = errorCode;
    }
    
    public String getFileId() {
        return fileId;
    }
    
    public Integer getChunkNumber() {
        return chunkNumber;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
