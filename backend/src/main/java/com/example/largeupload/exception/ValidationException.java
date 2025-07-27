package com.example.largeupload.exception;

import lombok.Getter;

import java.util.Map;

/**
 * Custom exception for validation errors
 */
@Getter
public class ValidationException extends RuntimeException {

    private static final String DEFAULT_ERROR_CODE = "VALIDATION_ERROR";

    private final String field;
    private final transient Object rejectedValue;
    private final String errorCode;
    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(message);
        this.field = null;
        this.rejectedValue = null;
        this.errorCode = DEFAULT_ERROR_CODE;
        this.fieldErrors = null;
    }

    public ValidationException(String message, String field, Object rejectedValue) {
        super(message);
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.errorCode = DEFAULT_ERROR_CODE;
        this.fieldErrors = null;
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.field = null;
        this.rejectedValue = null;
        this.errorCode = DEFAULT_ERROR_CODE;
        this.fieldErrors = fieldErrors;
    }
    
    public ValidationException(String message, String field, Object rejectedValue, String errorCode) {
        super(message);
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.errorCode = errorCode != null ? errorCode : DEFAULT_ERROR_CODE;
        this.fieldErrors = null;
    }
    

}
