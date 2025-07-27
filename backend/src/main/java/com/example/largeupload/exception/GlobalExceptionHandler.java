package com.example.largeupload.exception;

import com.example.largeupload.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for the application
 * Provides centralized error handling and logging
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
            ValidationException ex, ServerWebExchange exchange) {
        
        String traceId = generateTraceId();
        String path = exchange.getRequest().getPath().value();
        
        Map<String, Object> details = new HashMap<>();
        if (ex.getField() != null) {
            details.put("field", ex.getField());
            details.put("rejectedValue", ex.getRejectedValue());
        }
        if (ex.getFieldErrors() != null) {
            details.put("fieldErrors", ex.getFieldErrors());
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Error",
            ex.getMessage(),
            path,
            ex.getErrorCode(),
            details
        );
        errorResponse.setTraceId(traceId);
        
        logger.warn("Validation error [{}]: {} at path: {}, details: {}", 
                   traceId, ex.getMessage(), path, details, ex);
        
        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }
    
    @ExceptionHandler(FileUploadException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleFileUploadException(
            FileUploadException ex, ServerWebExchange exchange) {
        
        String traceId = generateTraceId();
        String path = exchange.getRequest().getPath().value();
        
        Map<String, Object> details = new HashMap<>();
        if (ex.getFileId() != null) {
            details.put("fileId", ex.getFileId());
        }
        if (ex.getChunkNumber() != null) {
            details.put("chunkNumber", ex.getChunkNumber());
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "File Upload Error",
            ex.getMessage(),
            path,
            ex.getErrorCode(),
            details
        );
        errorResponse.setTraceId(traceId);
        
        logger.error("File upload error [{}]: {} at path: {}, fileId: {}, chunkNumber: {}", 
                    traceId, ex.getMessage(), path, ex.getFileId(), ex.getChunkNumber(), ex);
        
        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }
    
    @ExceptionHandler(FileStorageException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleFileStorageException(
            FileStorageException ex, ServerWebExchange exchange) {
        
        String traceId = generateTraceId();
        String path = exchange.getRequest().getPath().value();
        
        Map<String, Object> details = new HashMap<>();
        if (ex.getFileId() != null) {
            details.put("fileId", ex.getFileId());
        }
        if (ex.getOperation() != null) {
            details.put("operation", ex.getOperation());
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "File Storage Error",
            ex.getMessage(),
            path,
            ex.getErrorCode(),
            details
        );
        errorResponse.setTraceId(traceId);
        
        logger.error("File storage error [{}]: {} at path: {}, fileId: {}, operation: {}", 
                    traceId, ex.getMessage(), path, ex.getFileId(), ex.getOperation(), ex);
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgumentException(
            IllegalArgumentException ex, ServerWebExchange exchange) {
        
        String traceId = generateTraceId();
        String path = exchange.getRequest().getPath().value();
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid Argument",
            ex.getMessage(),
            path,
            "INVALID_ARGUMENT"
        );
        errorResponse.setTraceId(traceId);
        
        logger.warn("Invalid argument error [{}]: {} at path: {}", traceId, ex.getMessage(), path, ex);
        
        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }
    
    @ExceptionHandler(IOException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIOException(
            IOException ex, ServerWebExchange exchange) {
        
        String traceId = generateTraceId();
        String path = exchange.getRequest().getPath().value();
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "I/O Error",
            "An I/O error occurred while processing the request",
            path,
            "IO_ERROR"
        );
        errorResponse.setTraceId(traceId);
        
        logger.error("I/O error [{}]: {} at path: {}", traceId, ex.getMessage(), path, ex);
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }
    
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebExchangeBindException(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        
        String traceId = generateTraceId();
        String path = exchange.getRequest().getPath().value();
        
        Map<String, Object> details = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        details.put("fieldErrors", fieldErrors);
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Binding Error",
            "Request binding failed",
            path,
            "BINDING_ERROR",
            details
        );
        errorResponse.setTraceId(traceId);
        
        logger.warn("Binding error [{}]: {} at path: {}, fieldErrors: {}", 
                   traceId, ex.getMessage(), path, fieldErrors, ex);
        
        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }
    
    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleServerWebInputException(
            ServerWebInputException ex, ServerWebExchange exchange) {
        
        String traceId = generateTraceId();
        String path = exchange.getRequest().getPath().value();
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid Input",
            "Invalid request input: " + ex.getReason(),
            path,
            "INVALID_INPUT"
        );
        errorResponse.setTraceId(traceId);
        
        logger.warn("Invalid input error [{}]: {} at path: {}", traceId, ex.getMessage(), path, ex);
        
        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }
    
    @ExceptionHandler(DecodingException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDecodingException(
            DecodingException ex, ServerWebExchange exchange) {
        
        String traceId = generateTraceId();
        String path = exchange.getRequest().getPath().value();
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Decoding Error",
            "Failed to decode request body",
            path,
            "DECODING_ERROR"
        );
        errorResponse.setTraceId(traceId);
        
        logger.warn("Decoding error [{}]: {} at path: {}", traceId, ex.getMessage(), path, ex);
        
        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }
    
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(
            Exception ex, ServerWebExchange exchange) {
        
        String traceId = generateTraceId();
        String path = exchange.getRequest().getPath().value();
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred",
            path,
            "INTERNAL_ERROR"
        );
        errorResponse.setTraceId(traceId);
        
        logger.error("Unexpected error [{}]: {} at path: {}", traceId, ex.getMessage(), path, ex);
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }
    
    private String generateTraceId() {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        return traceId;
    }
}
