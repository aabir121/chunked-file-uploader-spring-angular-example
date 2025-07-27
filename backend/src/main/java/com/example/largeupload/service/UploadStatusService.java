package com.example.largeupload.service;

import com.example.largeupload.model.FileUploadStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for managing upload status tracking
 */
@Service
public class UploadStatusService {

    private static final Logger logger = LoggerFactory.getLogger(UploadStatusService.class);
    
    private final Map<String, FileUploadStatus> fileUploadStatusMap = new ConcurrentHashMap<>();

    /**
     * Gets or creates upload status for a file
     */
    public FileUploadStatus getOrCreateUploadStatus(String fileId, int totalChunks) {
        return fileUploadStatusMap.computeIfAbsent(fileId, k -> {
            logger.debug("Creating new upload status for fileId: {} with {} total chunks", fileId, totalChunks);
            return new FileUploadStatus(fileId, totalChunks);
        });
    }

    /**
     * Gets or creates upload status for a file with additional metadata
     */
    public FileUploadStatus getOrCreateUploadStatus(String fileId, int totalChunks, String fileName, Long fileSize, Integer chunkSize) {
        return fileUploadStatusMap.computeIfAbsent(fileId, k -> {
            logger.debug("Creating new upload status for fileId: {} with {} total chunks, fileName: {}, fileSize: {}, chunkSize: {}",
                        fileId, totalChunks, fileName, fileSize, chunkSize);
            return new FileUploadStatus(fileId, totalChunks, fileName, fileSize, chunkSize);
        });
    }

    /**
     * Gets upload status for a specific file
     */
    public FileUploadStatus getUploadStatus(String fileId) {
        return fileUploadStatusMap.get(fileId);
    }

    /**
     * Reactive version of getUploadStatus
     */
    public Mono<FileUploadStatus> getUploadStatusReactive(String fileId) {
        return Mono.fromCallable(() -> getUploadStatus(fileId))
                .filter(status -> status != null);
    }

    /**
     * Gets all upload statuses
     */
    public Collection<FileUploadStatus> getAllUploadStatuses() {
        return fileUploadStatusMap.values();
    }

    /**
     * Reactive version of getAllUploadStatuses
     */
    public Flux<FileUploadStatus> getAllUploadStatusesReactive() {
        return Flux.fromIterable(getAllUploadStatuses());
    }

    /**
     * Updates upload status with a new chunk
     */
    public void addChunk(String fileId, int chunkNumber) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        if (status != null) {
            status.addChunk(chunkNumber);
            logger.debug("Added chunk {} to upload status for fileId: {}. Progress: {}/{}",
                        chunkNumber, fileId, status.getReceivedChunks().size(), status.getTotalChunks());
        } else {
            logger.warn("Attempted to add chunk {} to non-existent upload status for fileId: {}", chunkNumber, fileId);
        }
    }

    /**
     * Updates upload status with a new chunk and its actual size
     */
    public void addChunk(String fileId, int chunkNumber, int chunkSize) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        if (status != null) {
            status.addChunk(chunkNumber, chunkSize);
            logger.debug("Added chunk {} (size: {} bytes) to upload status for fileId: {}. Progress: {}/{}",
                        chunkNumber, chunkSize, fileId, status.getReceivedChunks().size(), status.getTotalChunks());
        } else {
            logger.warn("Attempted to add chunk {} to non-existent upload status for fileId: {}", chunkNumber, fileId);
        }
    }

    /**
     * Sets the filename for an upload
     */
    public void setFileName(String fileId, String fileName) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        if (status != null && status.getFileName() == null && fileName != null) {
            status.setFileName(fileName);
            logger.debug("Set filename '{}' for fileId: {}", fileName, fileId);
        }
    }

    /**
     * Marks an upload as completed
     */
    public void markAsCompleted(String fileId) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        if (status != null) {
            status.markAsCompleted();
            logger.info("Marked upload as completed for fileId: {}", fileId);
        } else {
            logger.warn("Attempted to mark non-existent upload as completed for fileId: {}", fileId);
        }
    }

    /**
     * Marks an upload as failed
     */
    public void markAsFailed(String fileId, String errorMessage) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        if (status != null) {
            status.markAsFailed(errorMessage);
            logger.warn("Marked upload as failed for fileId: {} with error: {}", fileId, errorMessage);
        } else {
            logger.warn("Attempted to mark non-existent upload as failed for fileId: {}", fileId);
        }
    }

    /**
     * Removes upload status (cleanup)
     */
    public void removeUploadStatus(String fileId) {
        FileUploadStatus removed = fileUploadStatusMap.remove(fileId);
        if (removed != null) {
            logger.debug("Removed upload status for fileId: {}", fileId);
        }
    }

    /**
     * Checks if upload is complete
     */
    public boolean isUploadComplete(String fileId) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        return status != null && status.isComplete();
    }

    /**
     * Gets upload progress as percentage
     */
    public double getUploadProgress(String fileId) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        if (status == null) {
            return 0.0;
        }
        return (double) status.getReceivedChunks().size() / status.getTotalChunks() * 100.0;
    }

    /**
     * Gets missing chunk numbers as array (for backward compatibility)
     */
    public int[] getMissingChunksArray(String fileId) {
        Set<Integer> missingChunks = getMissingChunks(fileId);
        return missingChunks.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Gets the number of received chunks
     */
    public int getReceivedChunkCount(String fileId) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        return status != null ? status.getReceivedChunks().size() : 0;
    }

    /**
     * Checks if a specific chunk has been received
     */
    public boolean hasChunk(String fileId, int chunkNumber) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        return status != null && status.getReceivedChunks().contains(chunkNumber);
    }

    /**
     * Updates the total chunks count (in case it changes)
     */
    public void updateTotalChunks(String fileId, int newTotalChunks) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        if (status != null && status.getTotalChunks() != newTotalChunks) {
            logger.warn("Total chunks mismatch for fileId: {}. Expected: {}, Got: {}", 
                       fileId, status.getTotalChunks(), newTotalChunks);
            // For now, we'll log the warning but not update the count
            // In a production system, you might want to handle this differently
        }
    }

    /**
     * Gets missing chunks for a specific file (for resume functionality)
     */
    public Set<Integer> getMissingChunks(String fileId) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        return status != null ? status.getMissingChunks() : Collections.emptySet();
    }

    /**
     * Gets the next expected chunk for sequential upload
     */
    public int getNextExpectedChunk(String fileId) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        return status != null ? status.getNextExpectedChunk() : 0;
    }

    /**
     * Checks if an upload can be resumed
     */
    public boolean canResume(String fileId) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        return status != null && status.canResume();
    }

    /**
     * Gets upload progress percentage
     */
    public double getProgressPercentage(String fileId) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        return status != null ? status.getProgressPercentage() : 0.0;
    }

    /**
     * Gets current upload speed in bytes per second
     */
    public Double getUploadSpeed(String fileId) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        return status != null ? status.getUploadSpeed() : 0.0;
    }

    /**
     * Gets estimated remaining time in milliseconds
     */
    public Long getEstimatedRemainingTime(String fileId) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        return status != null ? status.getEstimatedRemainingTime() : null;
    }

    /**
     * Updates file metadata for an existing upload
     */
    public void updateFileMetadata(String fileId, String fileName, Long fileSize, Integer chunkSize) {
        FileUploadStatus status = fileUploadStatusMap.get(fileId);
        if (status != null) {
            if (fileName != null) {
                status.setFileName(fileName);
            }
            if (fileSize != null) {
                status.setFileSize(fileSize);
            }
            if (chunkSize != null) {
                status.setChunkSize(chunkSize);
            }
            logger.debug("Updated metadata for fileId: {} - fileName: {}, fileSize: {}, chunkSize: {}",
                        fileId, fileName, fileSize, chunkSize);
        }
    }

    /**
     * Gets all uploads that can be resumed
     */
    public Collection<FileUploadStatus> getResumableUploads() {
        return fileUploadStatusMap.values().stream()
                .filter(FileUploadStatus::canResume)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Reactive version of getResumableUploads
     */
    public Flux<FileUploadStatus> getResumableUploadsReactive() {
        return Flux.fromIterable(getResumableUploads());
    }

    /**
     * Gets upload statistics
     */
    public UploadStatistics getUploadStatistics() {
        int totalUploads = fileUploadStatusMap.size();
        long completedUploads = fileUploadStatusMap.values().stream()
                .mapToLong(status -> status.isComplete() ? 1 : 0)
                .sum();
        long failedUploads = fileUploadStatusMap.values().stream()
                .mapToLong(status -> status.isFailed() ? 1 : 0)
                .sum();
        long inProgressUploads = totalUploads - completedUploads - failedUploads;

        return new UploadStatistics(totalUploads, completedUploads, failedUploads, inProgressUploads);
    }

    /**
     * Cleans up old completed or failed uploads
     */
    public int cleanupOldUploads(long maxAgeMillis) {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;

        var iterator = fileUploadStatusMap.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            FileUploadStatus status = entry.getValue();
            
            if ((status.isComplete() || status.isFailed()) &&
                (currentTime - status.getLastUpdated().getTime()) >= maxAgeMillis) {
                iterator.remove();
                removedCount++;
                logger.debug("Cleaned up old upload status for fileId: {}", entry.getKey());
            }
        }

        if (removedCount > 0) {
            logger.info("Cleaned up {} old upload statuses", removedCount);
        }

        return removedCount;
    }

    /**
     * Statistics about uploads
     */
    public static class UploadStatistics {
        private final int totalUploads;
        private final long completedUploads;
        private final long failedUploads;
        private final long inProgressUploads;

        public UploadStatistics(int totalUploads, long completedUploads, long failedUploads, long inProgressUploads) {
            this.totalUploads = totalUploads;
            this.completedUploads = completedUploads;
            this.failedUploads = failedUploads;
            this.inProgressUploads = inProgressUploads;
        }

        public int getTotalUploads() { return totalUploads; }
        public long getCompletedUploads() { return completedUploads; }
        public long getFailedUploads() { return failedUploads; }
        public long getInProgressUploads() { return inProgressUploads; }
    }
}
