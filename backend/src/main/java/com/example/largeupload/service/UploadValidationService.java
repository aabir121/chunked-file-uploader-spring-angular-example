package com.example.largeupload.service;

import com.example.largeupload.config.FileUploadProperties;
import com.example.largeupload.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for validating upload requests and parameters
 */
@Service
public class UploadValidationService {

    private static final Logger logger = LoggerFactory.getLogger(UploadValidationService.class);
    private final FileUploadProperties properties;

    public UploadValidationService(FileUploadProperties properties) {
        this.properties = properties;
    }

    /**
     * Validates upload request parameters
     */
    public void validateUploadRequest(String fileId, String chunkNumber, String totalChunks, String fileName) {
        Map<String, String> errors = new HashMap<>();

        // Validate fileId
        if (fileId == null || fileId.trim().isEmpty()) {
            errors.put("fileId", "fileId is required and cannot be empty");
        } else if (fileId.length() > 255) {
            errors.put("fileId", "fileId cannot exceed 255 characters");
        }

        // Validate chunkNumber
        if (chunkNumber == null || chunkNumber.trim().isEmpty()) {
            errors.put("chunkNumber", "chunkNumber is required and cannot be empty");
        } else {
            try {
                int chunkNum = Integer.parseInt(chunkNumber);
                if (chunkNum < 0) {
                    errors.put("chunkNumber", "chunkNumber must be non-negative");
                }
            } catch (NumberFormatException e) {
                errors.put("chunkNumber", "chunkNumber must be a valid integer");
            }
        }

        // Validate totalChunks
        if (totalChunks == null || totalChunks.trim().isEmpty()) {
            errors.put("totalChunks", "totalChunks is required and cannot be empty");
        } else {
            try {
                int totalChunksNum = Integer.parseInt(totalChunks);
                if (totalChunksNum <= 0) {
                    errors.put("totalChunks", "totalChunks must be positive");
                } else if (totalChunksNum > properties.getChunk().getMaxCount()) {
                    errors.put("totalChunks", "totalChunks exceeds maximum allowed: " + properties.getChunk().getMaxCount());
                }
            } catch (NumberFormatException e) {
                errors.put("totalChunks", "totalChunks must be a valid integer");
            }
        }

        // Validate fileName if provided
        if (fileName != null && !fileName.trim().isEmpty()) {
            validateFileName(fileName, errors);
        }

        // Validate chunk number is within total chunks range
        if (errors.isEmpty()) {
            try {
                int chunkNum = Integer.parseInt(chunkNumber);
                int totalChunksNum = Integer.parseInt(totalChunks);
                if (chunkNum >= totalChunksNum) {
                    errors.put("chunkNumber", "chunkNumber must be less than totalChunks");
                }
            } catch (NumberFormatException e) {
                // Already handled above
            }
        }

        if (!errors.isEmpty()) {
            logger.warn("Validation failed for upload request - fileId: {}, errors: {}", fileId, errors);
            throw new ValidationException("Validation failed", errors);
        }
    }

    /**
     * Validates file name and extension
     */
    public void validateFileName(String fileName, Map<String, String> errors) {
        if (fileName == null || fileName.trim().isEmpty()) {
            errors.put("fileName", "fileName cannot be empty");
            return;
        }

        // Check for invalid characters
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            errors.put("fileName", "fileName contains invalid characters");
        }

        // Check file extension
        String extension = getFileExtension(fileName);
        if (extension != null) {
            // Check blocked extensions
            if (properties.getValidation().getBlockedExtensions().contains(extension.toLowerCase())) {
                errors.put("fileName", "File extension '" + extension + "' is not allowed");
            }

            // Check allowed extensions (if specified)
            if (!properties.getValidation().getAllowedExtensions().isEmpty() &&
                !properties.getValidation().getAllowedExtensions().contains(extension.toLowerCase())) {
                errors.put("fileName", "File extension '" + extension + "' is not in allowed list");
            }
        }
    }

    /**
     * Validates chunk data
     */
    public void validateChunkData(byte[] chunkData, String fileId) {
        if (chunkData == null || chunkData.length == 0) {
            throw new ValidationException("Chunk data cannot be empty", "chunkData", "empty");
        }

        if (chunkData.length > properties.getChunk().getMaxSize()) {
            throw new ValidationException(
                "Chunk size exceeds maximum allowed: " + properties.getChunk().getMaxSize(),
                "chunkSize",
                chunkData.length
            );
        }
    }

    /**
     * Validates total file size based on chunk information
     */
    public void validateTotalFileSize(int totalChunks, long averageChunkSize) {
        long estimatedFileSize = totalChunks * averageChunkSize;
        if (estimatedFileSize > properties.getFile().getMaxSize()) {
            throw new ValidationException(
                "Estimated file size exceeds maximum allowed: " + properties.getFile().getMaxSize(),
                "fileSize",
                estimatedFileSize
            );
        }
    }

    /**
     * Extracts file extension from filename
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return null;
    }

    /**
     * Validates parsed integer parameters
     */
    public void validateParsedParameters(int chunkNumber, int totalChunks, String fileId) {
        if (chunkNumber < 0) {
            throw new ValidationException("Chunk number must be non-negative", "chunkNumber", chunkNumber);
        }

        if (totalChunks <= 0) {
            throw new ValidationException("Total chunks must be positive", "totalChunks", totalChunks);
        }

        if (chunkNumber >= totalChunks) {
            throw new ValidationException("Chunk number must be less than total chunks", "chunkNumber", chunkNumber);
        }

        if (totalChunks > properties.getChunk().getMaxCount()) {
            throw new ValidationException(
                "Total chunks exceeds maximum allowed: " + properties.getChunk().getMaxCount(),
                "totalChunks",
                totalChunks
            );
        }
    }
}
