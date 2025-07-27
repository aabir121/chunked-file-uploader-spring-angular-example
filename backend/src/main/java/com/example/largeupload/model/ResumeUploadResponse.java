package com.example.largeupload.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
public class ResumeUploadResponse {
    
    private String fileId;
    private int totalChunks;
    private String fileName;
    private Long fileSize;
    private Integer chunkSize;
    private Set<Integer> receivedChunks;
    private Set<Integer> missingChunks;
    private int nextExpectedChunk;
    private double progressPercentage;
    private long uploadedBytes;
    private boolean canResume;
    private boolean completed;
    private boolean failed;
    private String errorMessage;
    private Date createdAt;
    private Date lastUpdated;
    
    public ResumeUploadResponse() {
    }
    
    public ResumeUploadResponse(FileUploadStatus status) {
        this.fileId = status.getFileId();
        this.totalChunks = status.getTotalChunks();
        this.fileName = status.getFileName();
        this.fileSize = status.getFileSize();
        this.chunkSize = status.getChunkSize();
        this.receivedChunks = status.getReceivedChunks();
        this.missingChunks = status.getMissingChunks();
        this.nextExpectedChunk = status.getNextExpectedChunk();
        this.progressPercentage = status.getProgressPercentage();
        this.uploadedBytes = status.getUploadedBytes();
        this.canResume = status.canResume();
        this.completed = status.isComplete();
        this.failed = status.isFailed();
        this.errorMessage = status.getErrorMessage();
        this.createdAt = status.getCreatedAt();
        this.lastUpdated = status.getLastUpdated();
    }
}
