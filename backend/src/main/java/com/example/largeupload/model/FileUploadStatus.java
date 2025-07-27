package com.example.largeupload.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class FileUploadStatus {

    private final String fileId;
    private final int totalChunks;
    private final Set<Integer> receivedChunks;
    @Setter
    private String fileName; // Store original filename
    @Setter
    private Long fileSize; // Total file size in bytes
    @Setter
    private Integer chunkSize; // Size of each chunk in bytes
    private final AtomicLong uploadedBytes; // Total bytes uploaded so far
    private boolean completed;
    private boolean failed;
    private String errorMessage;
    private final Date createdAt;
    private Date lastUpdated;

    @JsonCreator
    public FileUploadStatus(@JsonProperty("fileId") String fileId, @JsonProperty("totalChunks") int totalChunks) {
        this.fileId = fileId;
        this.totalChunks = totalChunks;
        this.receivedChunks = Collections.synchronizedSet(new HashSet<>());
        this.uploadedBytes = new AtomicLong(0);
        this.completed = false;
        this.failed = false;
        this.createdAt = new Date();
        this.lastUpdated = new Date();
    }

    public FileUploadStatus(String fileId, int totalChunks, String fileName, Long fileSize, Integer chunkSize) {
        this.fileId = fileId;
        this.totalChunks = totalChunks;
        this.receivedChunks = Collections.synchronizedSet(new HashSet<>());
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.chunkSize = chunkSize;
        this.uploadedBytes = new AtomicLong(0);
        this.completed = false;
        this.failed = false;
        this.createdAt = new Date();
        this.lastUpdated = new Date();
    }

    public Set<Integer> getReceivedChunks() {
        return Collections.unmodifiableSet(receivedChunks);
    }

    public void addChunk(int chunkNumber) {
        if (receivedChunks.add(chunkNumber)) {
            // Only update uploaded bytes if this is a new chunk
            if (chunkSize != null) {
                uploadedBytes.addAndGet(chunkSize);
            }
        }
        this.lastUpdated = new Date();
    }

    public void addChunk(int chunkNumber, int actualChunkSize) {
        if (receivedChunks.add(chunkNumber)) {
            // Only update uploaded bytes if this is a new chunk
            uploadedBytes.addAndGet(actualChunkSize);
        }
        this.lastUpdated = new Date();
    }

    public boolean isComplete() {
        return completed || receivedChunks.size() == totalChunks;
    }

    public void markAsCompleted() {
        this.completed = true;
        this.lastUpdated = new Date();
    }

    public void markAsFailed(String errorMessage) {
        this.failed = true;
        this.errorMessage = errorMessage;
        this.lastUpdated = new Date();
    }

    public Date getCreatedAt() {
        return new Date(createdAt.getTime());
    }

    public Date getLastUpdated() {
        return new Date(lastUpdated.getTime());
    }

    /**
     * Gets the current uploaded bytes count
     */
    @JsonProperty("uploadedBytes")
    public long getUploadedBytes() {
        return uploadedBytes.get();
    }

    /**
     * Gets upload progress as a percentage (0.0 to 100.0)
     */
    @JsonProperty("progressPercentage")
    public double getProgressPercentage() {
        if (fileSize != null && fileSize > 0) {
            return (double) uploadedBytes.get() / fileSize * 100.0;
        } else if (totalChunks > 0) {
            return (double) receivedChunks.size() / totalChunks * 100.0;
        }
        return 0.0;
    }

    /**
     * Gets missing chunk numbers for resume functionality
     */
    @JsonProperty("missingChunks")
    public Set<Integer> getMissingChunks() {
        Set<Integer> missing = new HashSet<>();
        for (int i = 0; i < totalChunks; i++) {
            if (!receivedChunks.contains(i)) {
                missing.add(i);
            }
        }
        return missing;
    }

    /**
     * Gets the next expected chunk number for sequential uploads
     */
    @JsonProperty("nextExpectedChunk")
    public int getNextExpectedChunk() {
        for (int i = 0; i < totalChunks; i++) {
            if (!receivedChunks.contains(i)) {
                return i;
            }
        }
        return totalChunks; // All chunks received
    }

    /**
     * Checks if upload can be resumed (not failed and not completed)
     */
    @JsonProperty("canResume")
    public boolean canResume() {
        return !failed && !completed && receivedChunks.size() < totalChunks;
    }

    /**
     * Gets estimated remaining time in milliseconds based on upload speed
     */
    @JsonProperty("estimatedRemainingTime")
    public Long getEstimatedRemainingTime() {
        if (fileSize == null || uploadedBytes.get() == 0) {
            return null;
        }

        long elapsedTime = new Date().getTime() - createdAt.getTime();
        long remainingBytes = fileSize - uploadedBytes.get();

        if (remainingBytes <= 0) {
            return 0L;
        }

        double uploadSpeed = (double) uploadedBytes.get() / elapsedTime; // bytes per ms
        return (long) (remainingBytes / uploadSpeed);
    }

    /**
     * Gets current upload speed in bytes per second
     */
    @JsonProperty("uploadSpeed")
    public Double getUploadSpeed() {
        if (uploadedBytes.get() == 0) {
            return 0.0;
        }

        long elapsedTime = new Date().getTime() - createdAt.getTime();
        if (elapsedTime == 0) {
            return 0.0;
        }

        return (double) uploadedBytes.get() / elapsedTime * 1000; // bytes per second
    }
}