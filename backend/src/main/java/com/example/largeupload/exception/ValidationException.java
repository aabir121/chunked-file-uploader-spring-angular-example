package com.example.largeupload.exception;

import java.util.List;
import java.util.Map;

/**
 * Custom exception for validation errors
 */
public class ValidationException extends RuntimeException {
    
    private final String field;
    private final Object rejectedValue;
    private final String errorCode;
    private final Map<String, String> fieldErrors;
    
    public ValidationException(String message) {
        super(message);
        this.field = null;
        this.rejectedValue = null;
        this.errorCode = "VALIDATION_ERROR";
        this.fieldErrors = null;
    }
    
    public ValidationException(String message, String field, Object rejectedValue) {
        super(message);
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.errorCode = "VALIDATION_ERROR";
        this.fieldErrors = null;
    }
    
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.field = null;
        this.rejectedValue = null;
        this.errorCode = "VALIDATION_ERROR";
        this.fieldErrors = fieldErrors;
    }
    
    public ValidationException(String message, String field, Object rejectedValue, String errorCode) {
        super(message);
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.errorCode = errorCode;
        this.fieldErrors = null;
    }
    
    public String getField() {
        return field;
    }
    
    public Object getRejectedValue() {
        return rejectedValue;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
