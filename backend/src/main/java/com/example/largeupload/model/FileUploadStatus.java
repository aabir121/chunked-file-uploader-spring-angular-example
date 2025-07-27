package com.example.largeupload.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FileUploadStatus {

    private final String fileId;
    private final int totalChunks;
    private final Set<Integer> receivedChunks;

    public FileUploadStatus(String fileId, int totalChunks) {
        this.fileId = fileId;
        this.totalChunks = totalChunks;
        this.receivedChunks = Collections.synchronizedSet(new HashSet<>());
    }

    public String getFileId() {
        return fileId;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public Set<Integer> getReceivedChunks() {
        return Collections.unmodifiableSet(receivedChunks);
    }

    public void addChunk(int chunkNumber) {
        receivedChunks.add(chunkNumber);
    }

    public boolean isComplete() {
        return receivedChunks.size() == totalChunks;
    }
}