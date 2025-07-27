package com.example.largeupload.service;

import com.example.largeupload.config.FileUploadProperties;
import com.example.largeupload.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UploadValidationServiceTest {

    private UploadValidationService validationService;
    private FileUploadProperties properties;

    @BeforeEach
    void setUp() {
        properties = new FileUploadProperties();
        properties.getChunk().setMaxCount(1000);
        properties.getChunk().setMaxSize(10 * 1024 * 1024); // 10MB
        properties.getFile().setMaxSize(1024L * 1024 * 1024); // 1GB
        properties.getValidation().setBlockedExtensions(List.of("exe", "bat", "cmd"));
        properties.getValidation().setAllowedExtensions(List.of()); // Empty means all allowed except blocked
        
        validationService = new UploadValidationService(properties);
    }

    @Test
    void testValidateUploadRequestValid() {
        assertDoesNotThrow(() -> 
            validationService.validateUploadRequest("file123", "0", "5", "test.txt"));
    }

    @Test
    void testValidateUploadRequestEmptyFileId() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateUploadRequest("", "0", "5", "test.txt"));
        
        assertNotNull(exception.getFieldErrors());
        assertTrue(exception.getFieldErrors().containsKey("fileId"));
    }

    @Test
    void testValidateUploadRequestNullFileId() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateUploadRequest(null, "0", "5", "test.txt"));
        
        assertNotNull(exception.getFieldErrors());
        assertTrue(exception.getFieldErrors().containsKey("fileId"));
    }

    @Test
    void testValidateUploadRequestInvalidChunkNumber() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateUploadRequest("file123", "invalid", "5", "test.txt"));
        
        assertNotNull(exception.getFieldErrors());
        assertTrue(exception.getFieldErrors().containsKey("chunkNumber"));
    }

    @Test
    void testValidateUploadRequestNegativeChunkNumber() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateUploadRequest("file123", "-1", "5", "test.txt"));
        
        assertNotNull(exception.getFieldErrors());
        assertTrue(exception.getFieldErrors().containsKey("chunkNumber"));
    }

    @Test
    void testValidateUploadRequestInvalidTotalChunks() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateUploadRequest("file123", "0", "invalid", "test.txt"));
        
        assertNotNull(exception.getFieldErrors());
        assertTrue(exception.getFieldErrors().containsKey("totalChunks"));
    }

    @Test
    void testValidateUploadRequestZeroTotalChunks() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateUploadRequest("file123", "0", "0", "test.txt"));
        
        assertNotNull(exception.getFieldErrors());
        assertTrue(exception.getFieldErrors().containsKey("totalChunks"));
    }

    @Test
    void testValidateUploadRequestChunkNumberGreaterThanTotal() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateUploadRequest("file123", "5", "5", "test.txt"));
        
        assertNotNull(exception.getFieldErrors());
        assertTrue(exception.getFieldErrors().containsKey("chunkNumber"));
    }

    @Test
    void testValidateUploadRequestTooManyChunks() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateUploadRequest("file123", "0", "2000", "test.txt"));
        
        assertNotNull(exception.getFieldErrors());
        assertTrue(exception.getFieldErrors().containsKey("totalChunks"));
    }

    @Test
    void testValidateFileNameWithBlockedExtension() {
        Map<String, String> errors = new java.util.HashMap<>();
        validationService.validateFileName("malware.exe", errors);
        
        assertTrue(errors.containsKey("fileName"));
        assertTrue(errors.get("fileName").contains("not allowed"));
    }

    @Test
    void testValidateFileNameWithAllowedExtension() {
        Map<String, String> errors = new java.util.HashMap<>();
        validationService.validateFileName("document.txt", errors);
        
        assertFalse(errors.containsKey("fileName"));
    }

    @Test
    void testValidateFileNameWithInvalidCharacters() {
        Map<String, String> errors = new java.util.HashMap<>();
        validationService.validateFileName("../../../etc/passwd", errors);
        
        assertTrue(errors.containsKey("fileName"));
        assertTrue(errors.get("fileName").contains("invalid characters"));
    }

    @Test
    void testValidateFileNameEmpty() {
        Map<String, String> errors = new java.util.HashMap<>();
        validationService.validateFileName("", errors);
        
        assertTrue(errors.containsKey("fileName"));
    }

    @Test
    void testValidateFileNameNull() {
        Map<String, String> errors = new java.util.HashMap<>();
        validationService.validateFileName(null, errors);
        
        assertTrue(errors.containsKey("fileName"));
    }

    @Test
    void testValidateChunkDataValid() {
        byte[] validChunk = "Valid chunk data".getBytes();
        assertDoesNotThrow(() -> 
            validationService.validateChunkData(validChunk, "file123"));
    }

    @Test
    void testValidateChunkDataEmpty() {
        byte[] emptyChunk = new byte[0];
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateChunkData(emptyChunk, "file123"));
        
        assertEquals("chunkData", exception.getField());
    }

    @Test
    void testValidateChunkDataNull() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateChunkData(null, "file123"));
        
        assertEquals("chunkData", exception.getField());
    }

    @Test
    void testValidateChunkDataTooLarge() {
        byte[] largeChunk = new byte[15 * 1024 * 1024]; // 15MB, larger than 10MB limit
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateChunkData(largeChunk, "file123"));
        
        assertEquals("chunkSize", exception.getField());
    }

    @Test
    void testValidateParsedParametersValid() {
        assertDoesNotThrow(() -> 
            validationService.validateParsedParameters(0, 5, "file123"));
    }

    @Test
    void testValidateParsedParametersNegativeChunk() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateParsedParameters(-1, 5, "file123"));
        
        assertEquals("chunkNumber", exception.getField());
    }

    @Test
    void testValidateParsedParametersZeroTotalChunks() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateParsedParameters(0, 0, "file123"));
        
        assertEquals("totalChunks", exception.getField());
    }

    @Test
    void testValidateParsedParametersChunkGreaterThanTotal() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateParsedParameters(5, 5, "file123"));
        
        assertEquals("chunkNumber", exception.getField());
    }

    @Test
    void testValidateParsedParametersTooManyChunks() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateParsedParameters(0, 2000, "file123"));
        
        assertEquals("totalChunks", exception.getField());
    }

    @Test
    void testValidateTotalFileSize() {
        // Test with acceptable file size
        assertDoesNotThrow(() -> 
            validationService.validateTotalFileSize(100, 1024 * 1024)); // 100MB total
        
        // Test with file size too large
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateTotalFileSize(2000, 1024 * 1024)); // 2GB total
        
        assertEquals("fileSize", exception.getField());
    }

    @Test
    void testValidateUploadRequestWithAllowedExtensions() {
        // Set up properties with specific allowed extensions
        properties.getValidation().setAllowedExtensions(List.of("txt", "pdf", "jpg"));
        properties.getValidation().setBlockedExtensions(List.of());
        validationService = new UploadValidationService(properties);

        // Test allowed extension
        assertDoesNotThrow(() -> 
            validationService.validateUploadRequest("file123", "0", "5", "document.txt"));

        // Test disallowed extension
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateUploadRequest("file123", "0", "5", "script.js"));
        
        assertNotNull(exception.getFieldErrors());
        assertTrue(exception.getFieldErrors().containsKey("fileName"));
    }

    @Test
    void testValidateUploadRequestLongFileId() {
        String longFileId = "a".repeat(300); // Longer than 255 characters
        ValidationException exception = assertThrows(ValidationException.class, () ->
            validationService.validateUploadRequest(longFileId, "0", "5", "test.txt"));
        
        assertNotNull(exception.getFieldErrors());
        assertTrue(exception.getFieldErrors().containsKey("fileId"));
    }
}
