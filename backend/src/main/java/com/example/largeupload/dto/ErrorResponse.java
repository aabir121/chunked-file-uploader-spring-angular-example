package com.example.largeupload.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized error response structure
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("status")
    private int status;
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("path")
    private String path;
    
    @JsonProperty("errorCode")
    private String errorCode;
    
    @JsonProperty("details")
    private Map<String, Object> details;
    
    @JsonProperty("traceId")
    private String traceId;
    
    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ErrorResponse(int status, String error, String message, String path) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
    
    public ErrorResponse(int status, String error, String message, String path, String errorCode) {
        this(status, error, message, path);
        this.errorCode = errorCode;
    }
    
    public ErrorResponse(int status, String error, String message, String path, String errorCode, Map<String, Object> details) {
        this(status, error, message, path, errorCode);
        this.details = details;
    }
    
    // Getters and setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public Map<String, Object> getDetails() {
        return details;
    }
    
    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
