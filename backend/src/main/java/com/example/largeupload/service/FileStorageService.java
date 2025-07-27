package com.example.largeupload.service;

import com.example.largeupload.exception.FileStorageException;
import com.example.largeupload.exception.ValidationException;
import com.example.largeupload.model.FileUploadStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private final Path uploadDir = Paths.get("uploads");
    private final Map<String, FileUploadStatus> fileUploadStatusMap = new ConcurrentHashMap<>();

    public FileStorageService() {
        try {
            Files.createDirectories(uploadDir);
            logger.info("Upload directory created/verified at: {}", uploadDir.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create upload directory at: {}", uploadDir.toAbsolutePath(), e);
            throw new FileStorageException("Could not create upload directory", null, "INIT", "DIRECTORY_CREATION_FAILED", e);
        }
    }

    public void saveChunk(String fileId, int chunkNumber, int totalChunks, byte[] chunk, String fileName) throws IOException {
        logger.debug("Saving chunk {} of {} for fileId: {}, chunk size: {} bytes, fileName: {}",
                    chunkNumber, totalChunks, fileId, chunk.length, fileName);

        FileUploadStatus status = fileUploadStatusMap.computeIfAbsent(fileId, k -> new FileUploadStatus(fileId, totalChunks));

        // Store the filename if not already set
        if (status.getFileName() == null && fileName != null) {
            status.setFileName(fileName);
        }

        Path chunkPath = uploadDir.resolve(fileId + ".part" + chunkNumber);
        try {
            Files.write(chunkPath, chunk, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            logger.debug("Successfully saved chunk {} for fileId: {} at path: {}",
                        chunkNumber, fileId, chunkPath);
        } catch (IOException e) {
            logger.error("Failed to save chunk {} for fileId: {} at path: {}",
                        chunkNumber, fileId, chunkPath, e);
            throw new FileStorageException("Failed to save chunk", fileId, "SAVE_CHUNK", "CHUNK_WRITE_FAILED", e);
        }

        status.addChunk(chunkNumber);

        // Note: File combination is now done manually via the complete API
        // This allows for better error handling and explicit completion control
        logger.debug("Chunk {} saved for fileId: {}. Total chunks received: {}/{}",
                    chunkNumber, fileId, status.getReceivedChunks().size(), totalChunks);
    }

    // Overloaded method for backward compatibility
    public void saveChunk(String fileId, int chunkNumber, int totalChunks, byte[] chunk) throws IOException {
        saveChunk(fileId, chunkNumber, totalChunks, chunk, null);
    }

    /**
     * Manually complete the upload by combining all chunks into the final file.
     * This method should be called after all chunks have been uploaded.
     */
    public void completeUpload(String fileId) throws IOException {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        if (status == null) {
            throw new IllegalStateException("Upload not found for fileId: " + fileId);
        }

        if (!status.isComplete()) {
            throw new IllegalStateException("Upload is not complete. Missing chunks: " +
                (status.getTotalChunks() - status.getReceivedChunks().size()) + "/" + status.getTotalChunks());
        }

        combineChunks(fileId, status.getTotalChunks());
    }

    private void combineChunks(String fileId, int totalChunks) throws IOException {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        String fileName = (status != null && status.getFileName() != null) ? status.getFileName() : fileId;

        Path finalFilePath = uploadDir.resolve(fileName);
        logger.info("Combining {} chunks for fileId: {} into final file: {} (original name: {})",
                   totalChunks, fileId, finalFilePath, fileName);

        try {
            for (int i = 0; i < totalChunks; i++) {
                Path chunkPath = uploadDir.resolve(fileId + ".part" + i);
                if (!Files.exists(chunkPath)) {
                    throw new FileStorageException("Missing chunk file", fileId, "COMBINE_CHUNKS", "MISSING_CHUNK", null);
                }

                byte[] chunk = Files.readAllBytes(chunkPath);
                Files.write(finalFilePath, chunk, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                Files.delete(chunkPath);
                logger.debug("Combined and deleted chunk {} for fileId: {}", i, fileId);
            }

            fileUploadStatusMap.remove(fileId);
            logger.info("Successfully combined all chunks for fileId: {}, final file size: {} bytes",
                       fileId, Files.size(finalFilePath));

        } catch (IOException e) {
            logger.error("Failed to combine chunks for fileId: {}", fileId, e);
            throw new FileStorageException("Failed to combine chunks", fileId, "COMBINE_CHUNKS", "COMBINATION_FAILED", e);
        }
    }

    public FileUploadStatus getUploadStatus(String fileId) {
        return fileUploadStatusMap.get(fileId);
    }

    public Collection<FileUploadStatus> getAllUploadStatuses() {
        return fileUploadStatusMap.values();
    }

    // Reactive methods for WebFlux

    /**
     * Reactive version of saveChunk that returns a Mono<Void>
     */
    public Mono<Void> saveChunkReactive(String fileId, int chunkNumber, int totalChunks, byte[] chunk) {
        return Mono.fromCallable(() -> {
            try {
                saveChunk(fileId, chunkNumber, totalChunks, chunk);
                return null;
            } catch (IOException e) {
                logger.error("Reactive save chunk failed for fileId: {}, chunk: {}", fileId, chunkNumber, e);
                throw new FileStorageException("Failed to save chunk reactively", fileId, "SAVE_CHUNK_REACTIVE", "REACTIVE_SAVE_FAILED", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic()) // Use boundedElastic for I/O operations
        .then(); // Convert to Mono<Void>
    }

    /**
     * Reactive version of getUploadStatus that returns a Mono<FileUploadStatus>
     */
    public Mono<FileUploadStatus> getUploadStatusReactive(String fileId) {
        return Mono.fromCallable(() -> getUploadStatus(fileId))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Reactive version of getAllUploadStatuses that returns a Flux<FileUploadStatus>
     */
    public Flux<FileUploadStatus> getAllUploadStatusesReactive() {
        return Flux.fromIterable(getAllUploadStatuses())
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Enhanced reactive method that provides better error handling and validation
     */
    public Mono<Void> saveChunkReactiveEnhanced(String fileId, int chunkNumber, int totalChunks, byte[] chunk) {
        return Mono.fromCallable(() -> {
            // Validation
            if (fileId == null || fileId.trim().isEmpty()) {
                logger.warn("Validation failed: FileId is null or empty");
                throw new ValidationException("FileId cannot be null or empty", "fileId", fileId);
            }
            if (chunkNumber < 0) {
                logger.warn("Validation failed: Chunk number is negative: {}", chunkNumber);
                throw new ValidationException("Chunk number cannot be negative", "chunkNumber", chunkNumber);
            }
            if (totalChunks <= 0) {
                logger.warn("Validation failed: Total chunks is not positive: {}", totalChunks);
                throw new ValidationException("Total chunks must be positive", "totalChunks", totalChunks);
            }
            if (chunk == null || chunk.length == 0) {
                logger.warn("Validation failed: Chunk data is null or empty for fileId: {}", fileId);
                throw new ValidationException("Chunk data cannot be null or empty", "chunk", chunk != null ? chunk.length : null);
            }

            try {
                saveChunk(fileId, chunkNumber, totalChunks, chunk);
                return null;
            } catch (IOException e) {
                logger.error("Enhanced reactive save chunk failed for fileId: {}, chunk: {}", fileId, chunkNumber, e);
                throw new FileStorageException("Failed to save chunk for fileId: " + fileId + ", chunk: " + chunkNumber,
                                             fileId, "SAVE_CHUNK_ENHANCED", "ENHANCED_SAVE_FAILED", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    /**
     * Enhanced reactive method with filename support
     */
    public Mono<Void> saveChunkReactiveEnhanced(String fileId, int chunkNumber, int totalChunks, byte[] chunk, String fileName) {
        return Mono.fromCallable(() -> {
            // Validation
            if (fileId == null || fileId.trim().isEmpty()) {
                logger.warn("Validation failed: FileId is null or empty");
                throw new ValidationException("FileId cannot be null or empty", "fileId", fileId);
            }

            try {
                saveChunk(fileId, chunkNumber, totalChunks, chunk, fileName);
                return null;
            } catch (IOException e) {
                logger.error("Enhanced reactive save chunk with filename failed for fileId: {}, chunk: {}", fileId, chunkNumber, e);
                throw new FileStorageException("Failed to save chunk for fileId: " + fileId + ", chunk: " + chunkNumber,
                                             fileId, "SAVE_CHUNK_ENHANCED_WITH_FILENAME", "ENHANCED_SAVE_WITH_FILENAME_FAILED", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    /**
     * Reactive version of completeUpload that returns a Mono<Void>
     */
    public Mono<Void> completeUploadReactive(String fileId) {
        return Mono.fromCallable(() -> {
            try {
                completeUpload(fileId);
                return null;
            } catch (IOException e) {
                logger.error("Reactive complete upload failed for fileId: {}", fileId, e);
                throw new FileStorageException("Failed to complete upload for fileId: " + fileId,
                                             fileId, "COMPLETE_UPLOAD_REACTIVE", "REACTIVE_COMPLETE_FAILED", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
}