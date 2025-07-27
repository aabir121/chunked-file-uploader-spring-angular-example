package com.example.largeupload.exception;

import com.example.largeupload.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest
@ContextConfiguration(classes = {GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    void testHandleValidationException() {
        ValidationException ex = new ValidationException("Test validation error", "testField", "testValue");
        ServerWebExchange exchange = MockServerRequest.builder().build().exchange();
        
        Mono<org.springframework.http.ResponseEntity<ErrorResponse>> result = 
            globalExceptionHandler.handleValidationException(ex, exchange);
        
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                ErrorResponse errorResponse = response.getBody();
                assertNotNull(errorResponse);
                assertEquals("Validation Error", errorResponse.getError());
                assertEquals("Test validation error", errorResponse.getMessage());
                assertEquals("VALIDATION_ERROR", errorResponse.getErrorCode());
                assertNotNull(errorResponse.getTraceId());
            })
            .verifyComplete();
    }

    @Test
    void testHandleFileUploadException() {
        FileUploadException ex = new FileUploadException("Test upload error", "file123", 5);
        ServerWebExchange exchange = MockServerRequest.builder().build().exchange();
        
        Mono<org.springframework.http.ResponseEntity<ErrorResponse>> result = 
            globalExceptionHandler.handleFileUploadException(ex, exchange);
        
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                ErrorResponse errorResponse = response.getBody();
                assertNotNull(errorResponse);
                assertEquals("File Upload Error", errorResponse.getError());
                assertEquals("Test upload error", errorResponse.getMessage());
                assertEquals("UPLOAD_ERROR", errorResponse.getErrorCode());
                assertNotNull(errorResponse.getDetails());
                assertEquals("file123", errorResponse.getDetails().get("fileId"));
                assertEquals(5, errorResponse.getDetails().get("chunkNumber"));
            })
            .verifyComplete();
    }

    @Test
    void testHandleFileStorageException() {
        FileStorageException ex = new FileStorageException("Test storage error", "file123", "SAVE_CHUNK");
        ServerWebExchange exchange = MockServerRequest.builder().build().exchange();
        
        Mono<org.springframework.http.ResponseEntity<ErrorResponse>> result = 
            globalExceptionHandler.handleFileStorageException(ex, exchange);
        
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                ErrorResponse errorResponse = response.getBody();
                assertNotNull(errorResponse);
                assertEquals("File Storage Error", errorResponse.getError());
                assertEquals("Test storage error", errorResponse.getMessage());
                assertEquals("STORAGE_ERROR", errorResponse.getErrorCode());
                assertNotNull(errorResponse.getDetails());
                assertEquals("file123", errorResponse.getDetails().get("fileId"));
                assertEquals("SAVE_CHUNK", errorResponse.getDetails().get("operation"));
            })
            .verifyComplete();
    }

    @Test
    void testHandleGenericException() {
        Exception ex = new RuntimeException("Test generic error");
        ServerWebExchange exchange = MockServerRequest.builder().build().exchange();
        
        Mono<org.springframework.http.ResponseEntity<ErrorResponse>> result = 
            globalExceptionHandler.handleGenericException(ex, exchange);
        
        StepVerifier.create(result)
            .assertNext(response -> {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                ErrorResponse errorResponse = response.getBody();
                assertNotNull(errorResponse);
                assertEquals("Internal Server Error", errorResponse.getError());
                assertEquals("An unexpected error occurred", errorResponse.getMessage());
                assertEquals("INTERNAL_ERROR", errorResponse.getErrorCode());
                assertNotNull(errorResponse.getTraceId());
            })
            .verifyComplete();
    }
}
