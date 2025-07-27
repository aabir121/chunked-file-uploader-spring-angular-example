package com.example.largeupload.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Getter
public class FileUploadStatus {

    private final String fileId;
    private final int totalChunks;
    private final Set<Integer> receivedChunks;
    @Setter
    private String fileName; // Store original filename
    private boolean completed;
    private boolean failed;
    private String errorMessage;
    private final Date createdAt;
    private Date lastUpdated;

    public FileUploadStatus(String fileId, int totalChunks) {
        this.fileId = fileId;
        this.totalChunks = totalChunks;
        this.receivedChunks = Collections.synchronizedSet(new HashSet<>());
        this.completed = false;
        this.failed = false;
        this.createdAt = new Date();
        this.lastUpdated = new Date();
    }

    public Set<Integer> getReceivedChunks() {
        return Collections.unmodifiableSet(receivedChunks);
    }

    public void addChunk(int chunkNumber) {
        receivedChunks.add(chunkNumber);
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
}