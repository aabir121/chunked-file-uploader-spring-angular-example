package com.example.largeupload.service;

import com.example.largeupload.model.FileUploadStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Collection;

/**
 * Main service for file upload operations, orchestrating chunk storage, file assembly, and status tracking
 */
@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final ChunkStorageService chunkStorageService;
    private final FileAssemblyService fileAssemblyService;
    private final UploadStatusService uploadStatusService;

    public FileStorageService(ChunkStorageService chunkStorageService,
                             FileAssemblyService fileAssemblyService,
                             UploadStatusService uploadStatusService) {
        this.chunkStorageService = chunkStorageService;
        this.fileAssemblyService = fileAssemblyService;
        this.uploadStatusService = uploadStatusService;
    }

    /**
     * Saves a chunk with validation
     */
    public void saveChunk(String fileId, int chunkNumber, int totalChunks, byte[] chunk, String fileName) throws IOException {
        logger.debug("Saving chunk {} of {} for fileId: {}, chunk size: {} bytes, fileName: {}",
                    chunkNumber, totalChunks, fileId, chunk.length, fileName);

        // Basic validation
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new IllegalArgumentException("fileId cannot be null or empty");
        }
        if (chunkNumber < 0) {
            throw new IllegalArgumentException("chunkNumber cannot be negative");
        }
        if (totalChunks <= 0) {
            throw new IllegalArgumentException("totalChunks must be positive");
        }
        if (chunk == null || chunk.length == 0) {
            throw new IllegalArgumentException("chunk cannot be null or empty");
        }

        // Get or create upload status
        uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);

        // Set filename if provided and not already set
        if (fileName != null && !fileName.trim().isEmpty()) {
            uploadStatusService.setFileName(fileId, fileName);
        }

        // Save the chunk
        chunkStorageService.saveChunk(fileId, chunkNumber, chunk);

        // Update status
        uploadStatusService.addChunk(fileId, chunkNumber);

        logger.debug("Chunk {} saved for fileId: {}. Progress: {}/{}",
                    chunkNumber, fileId, uploadStatusService.getReceivedChunkCount(fileId), totalChunks);
    }

    /**
     * Overloaded method for backward compatibility
     */
    public void saveChunk(String fileId, int chunkNumber, int totalChunks, byte[] chunk) throws IOException {
        saveChunk(fileId, chunkNumber, totalChunks, chunk, null);
    }

    /**
     * Completes the upload by assembling all chunks into the final file
     */
    public void completeUpload(String fileId) throws IOException {
        logger.info("Completing upload for fileId: {}", fileId);

        FileUploadStatus status = uploadStatusService.getUploadStatus(fileId);
        if (status == null) {
            throw new IllegalArgumentException("No upload found for fileId: " + fileId);
        }

        if (!uploadStatusService.isUploadComplete(fileId)) {
            int[] missingChunks = uploadStatusService.getMissingChunks(fileId);
            throw new IllegalStateException("Upload is not complete. Missing chunks: " + java.util.Arrays.toString(missingChunks));
        }

        try {
            // Assemble the file
            fileAssemblyService.assembleFile(fileId, status.getTotalChunks(), status.getFileName());

            // Mark as completed
            uploadStatusService.markAsCompleted(fileId);

            // Clean up temporary files
            chunkStorageService.cleanupTempDirectory(fileId);

            logger.info("Successfully completed upload for fileId: {}", fileId);

        } catch (Exception e) {
            uploadStatusService.markAsFailed(fileId, "Failed to complete upload: " + e.getMessage());
            logger.error("Failed to complete upload for fileId: {}", fileId, e);
            throw e;
        }
    }

    /**
     * Reactive version of saveChunk
     */
    public Mono<Void> saveChunkReactive(String fileId, int chunkNumber, int totalChunks, byte[] chunk) {
        return Mono.fromCallable(() -> {
            try {
                saveChunk(fileId, chunkNumber, totalChunks, chunk);
                return null;
            } catch (IOException e) {
                logger.error("Reactive save chunk failed for fileId: {}, chunk: {}", fileId, chunkNumber, e);
                throw new RuntimeException("Failed to save chunk reactively", e);
            }
        });
    }

    /**
     * Enhanced reactive version with filename support
     */
    public Mono<Void> saveChunkReactiveEnhanced(String fileId, int chunkNumber, int totalChunks, byte[] chunk, String fileName) {
        return chunkStorageService.saveChunkReactive(fileId, chunkNumber, chunk)
                .doOnSubscribe(subscription -> {
                    // Basic validation
                    if (fileId == null || fileId.trim().isEmpty()) {
                        throw new IllegalArgumentException("fileId cannot be null or empty");
                    }
                    if (chunkNumber < 0) {
                        throw new IllegalArgumentException("chunkNumber cannot be negative");
                    }
                    if (totalChunks <= 0) {
                        throw new IllegalArgumentException("totalChunks must be positive");
                    }
                    if (chunk == null || chunk.length == 0) {
                        throw new IllegalArgumentException("chunk cannot be null or empty");
                    }

                    uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
                    if (fileName != null && !fileName.trim().isEmpty()) {
                        uploadStatusService.setFileName(fileId, fileName);
                    }
                    uploadStatusService.addChunk(fileId, chunkNumber);
                })
                .doOnSuccess(result -> logger.debug("Reactive chunk {} saved for fileId: {}", chunkNumber, fileId))
                .doOnError(error -> {
                    logger.error("Failed to save chunk {} for fileId: {}", chunkNumber, fileId, error);
                    uploadStatusService.markAsFailed(fileId, "Chunk save failed: " + error.getMessage());
                });
    }

    /**
     * Enhanced reactive version without filename (backward compatibility)
     */
    public Mono<Void> saveChunkReactiveEnhanced(String fileId, int chunkNumber, int totalChunks, byte[] chunk) {
        return saveChunkReactiveEnhanced(fileId, chunkNumber, totalChunks, chunk, null);
    }

    /**
     * Reactive version of completeUpload
     */
    public Mono<Void> completeUploadReactive(String fileId) {
        return Mono.fromCallable(() -> {
            try {
                completeUpload(fileId);
                return null;
            } catch (IOException e) {
                logger.error("Reactive complete upload failed for fileId: {}", fileId, e);
                throw new RuntimeException("Failed to complete upload reactively", e);
            }
        });
    }

    /**
     * Gets upload status for a specific file
     */
    public FileUploadStatus getUploadStatus(String fileId) {
        return uploadStatusService.getUploadStatus(fileId);
    }

    /**
     * Reactive version of getUploadStatus
     */
    public Mono<FileUploadStatus> getUploadStatusReactive(String fileId) {
        return uploadStatusService.getUploadStatusReactive(fileId);
    }

    /**
     * Gets all upload statuses
     */
    public Collection<FileUploadStatus> getAllUploadStatuses() {
        return uploadStatusService.getAllUploadStatuses();
    }

    /**
     * Reactive version of getAllUploadStatuses
     */
    public Flux<FileUploadStatus> getAllUploadStatusesReactive() {
        return uploadStatusService.getAllUploadStatusesReactive();
    }

    /**
     * Cleans up temporary directory for a specific fileId
     */
    public void cleanupTempDirectory(String fileId) {
        chunkStorageService.cleanupTempDirectory(fileId);
        uploadStatusService.removeUploadStatus(fileId);
    }

    /**
     * Reactive version of cleanup
     */
    public Mono<Void> cleanupTempDirectoryReactive(String fileId) {
        return chunkStorageService.cleanupTempDirectoryReactive(fileId)
                .doOnSuccess(result -> uploadStatusService.removeUploadStatus(fileId));
    }

    /**
     * Checks if all chunks have been received for a file
     */
    public boolean areAllChunksReceived(String fileId) {
        return uploadStatusService.isUploadComplete(fileId);
    }

    /**
     * Gets missing chunk numbers for a file
     */
    public int[] getMissingChunks(String fileId) {
        return uploadStatusService.getMissingChunks(fileId);
    }

    /**
     * Gets upload progress as percentage
     */
    public double getUploadProgress(String fileId) {
        return uploadStatusService.getUploadProgress(fileId);
    }

    /**
     * Validates if a file can be completed
     */
    public boolean canCompleteUpload(String fileId) {
        FileUploadStatus status = uploadStatusService.getUploadStatus(fileId);
        return status != null && uploadStatusService.isUploadComplete(fileId) &&
               fileAssemblyService.canAssemble(fileId, status.getTotalChunks());
    }
}