package com.example.largeupload.service;

import com.example.largeupload.model.FileUploadStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Resume Upload Functionality Tests")
class ResumeUploadTest {

    private UploadStatusService uploadStatusService;

    @BeforeEach
    void setUp() {
        uploadStatusService = new UploadStatusService();
    }

    @Test
    @DisplayName("Should create upload status with metadata")
    void testCreateUploadStatusWithMetadata() {
        String fileId = "test-metadata";
        int totalChunks = 10;
        String fileName = "test.pdf";
        Long fileSize = 1024000L;
        Integer chunkSize = 102400;

        FileUploadStatus status = uploadStatusService.getOrCreateUploadStatus(
            fileId, totalChunks, fileName, fileSize, chunkSize);

        assertNotNull(status);
        assertEquals(fileId, status.getFileId());
        assertEquals(totalChunks, status.getTotalChunks());
        assertEquals(fileName, status.getFileName());
        assertEquals(fileSize, status.getFileSize());
        assertEquals(chunkSize, status.getChunkSize());
        assertEquals(0, status.getUploadedBytes());
        assertTrue(status.canResume());
        assertFalse(status.isComplete());
    }

    @Test
    @DisplayName("Should track uploaded bytes correctly")
    void testUploadedBytesTracking() {
        String fileId = "test-bytes";
        int totalChunks = 5;
        Long fileSize = 500L;
        Integer chunkSize = 100;

        FileUploadStatus status = uploadStatusService.getOrCreateUploadStatus(
            fileId, totalChunks, null, fileSize, chunkSize);

        // Add chunks with actual sizes
        uploadStatusService.addChunk(fileId, 0, 100);
        uploadStatusService.addChunk(fileId, 1, 100);
        uploadStatusService.addChunk(fileId, 2, 100);

        assertEquals(300, status.getUploadedBytes());
        assertEquals(60.0, status.getProgressPercentage(), 0.1);
        assertTrue(status.canResume());
        assertFalse(status.isComplete());
    }

    @Test
    @DisplayName("Should identify missing chunks correctly")
    void testMissingChunksIdentification() {
        String fileId = "test-missing";
        int totalChunks = 10;

        uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);

        // Add some chunks
        uploadStatusService.addChunk(fileId, 0);
        uploadStatusService.addChunk(fileId, 2);
        uploadStatusService.addChunk(fileId, 4);
        uploadStatusService.addChunk(fileId, 9);

        Set<Integer> missingChunks = uploadStatusService.getMissingChunks(fileId);
        assertEquals(6, missingChunks.size());
        assertTrue(missingChunks.contains(1));
        assertTrue(missingChunks.contains(3));
        assertTrue(missingChunks.contains(5));
        assertTrue(missingChunks.contains(6));
        assertTrue(missingChunks.contains(7));
        assertTrue(missingChunks.contains(8));
        assertFalse(missingChunks.contains(0));
        assertFalse(missingChunks.contains(2));
        assertFalse(missingChunks.contains(4));
        assertFalse(missingChunks.contains(9));
    }

    @Test
    @DisplayName("Should find next expected chunk correctly")
    void testNextExpectedChunk() {
        String fileId = "test-next";
        int totalChunks = 5;

        uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
        FileUploadStatus status = uploadStatusService.getUploadStatus(fileId);

        // Initially, next expected chunk should be 0
        assertEquals(0, status.getNextExpectedChunk());

        // Add chunk 0, next should be 1
        uploadStatusService.addChunk(fileId, 0);
        assertEquals(1, status.getNextExpectedChunk());

        // Add chunk 1, next should be 2
        uploadStatusService.addChunk(fileId, 1);
        assertEquals(2, status.getNextExpectedChunk());

        // Skip chunk 2, add chunk 3, next should still be 2
        uploadStatusService.addChunk(fileId, 3);
        assertEquals(2, status.getNextExpectedChunk());

        // Add chunk 2, next should be 4
        uploadStatusService.addChunk(fileId, 2);
        assertEquals(4, status.getNextExpectedChunk());

        // Add final chunk, should return totalChunks
        uploadStatusService.addChunk(fileId, 4);
        assertEquals(totalChunks, status.getNextExpectedChunk());
    }

    @Test
    @DisplayName("Should handle resume capability correctly")
    void testResumeCapability() {
        String fileId = "test-resume";
        int totalChunks = 3;

        uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
        FileUploadStatus status = uploadStatusService.getUploadStatus(fileId);

        // Initially can resume
        assertTrue(status.canResume());

        // Add some chunks, still can resume
        uploadStatusService.addChunk(fileId, 0);
        assertTrue(status.canResume());

        // Complete upload, cannot resume
        uploadStatusService.addChunk(fileId, 1);
        uploadStatusService.addChunk(fileId, 2);
        uploadStatusService.markAsCompleted(fileId);
        assertFalse(status.canResume());

        // Test failed upload
        String failedFileId = "test-failed";
        uploadStatusService.getOrCreateUploadStatus(failedFileId, totalChunks);
        uploadStatusService.markAsFailed(failedFileId, "Test failure");
        FileUploadStatus failedStatus = uploadStatusService.getUploadStatus(failedFileId);
        assertFalse(failedStatus.canResume());
    }

    @Test
    @DisplayName("Should get resumable uploads correctly")
    void testGetResumableUploads() {
        // Create various upload states
        uploadStatusService.getOrCreateUploadStatus("resumable1", 5);
        uploadStatusService.addChunk("resumable1", 0);

        uploadStatusService.getOrCreateUploadStatus("resumable2", 3);
        uploadStatusService.addChunk("resumable2", 0);
        uploadStatusService.addChunk("resumable2", 1);

        uploadStatusService.getOrCreateUploadStatus("completed", 2);
        uploadStatusService.addChunk("completed", 0);
        uploadStatusService.addChunk("completed", 1);
        uploadStatusService.markAsCompleted("completed");

        uploadStatusService.getOrCreateUploadStatus("failed", 4);
        uploadStatusService.markAsFailed("failed", "Test failure");

        var resumableUploads = uploadStatusService.getResumableUploads();
        assertEquals(2, resumableUploads.size());

        boolean hasResumable1 = resumableUploads.stream()
            .anyMatch(status -> "resumable1".equals(status.getFileId()));
        boolean hasResumable2 = resumableUploads.stream()
            .anyMatch(status -> "resumable2".equals(status.getFileId()));

        assertTrue(hasResumable1);
        assertTrue(hasResumable2);
    }

    @Test
    @DisplayName("Should handle concurrent chunk uploads correctly")
    void testConcurrentChunkUploads() throws InterruptedException {
        String fileId = "test-concurrent";
        int totalChunks = 100;
        
        uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        // Submit concurrent chunk uploads
        CompletableFuture<?>[] futures = new CompletableFuture[totalChunks];
        for (int i = 0; i < totalChunks; i++) {
            final int chunkNumber = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                uploadStatusService.addChunk(fileId, chunkNumber, 1024);
            }, executor);
        }
        
        // Wait for all uploads to complete
        CompletableFuture.allOf(futures).join();
        
        FileUploadStatus status = uploadStatusService.getUploadStatus(fileId);
        assertEquals(totalChunks, status.getReceivedChunks().size());
        assertEquals(totalChunks * 1024, status.getUploadedBytes());
        assertTrue(status.isComplete());
        assertEquals(0, status.getMissingChunks().size());
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should calculate upload speed and remaining time")
    void testUploadSpeedAndRemainingTime() throws InterruptedException {
        String fileId = "test-speed";
        int totalChunks = 10;
        Long fileSize = 10240L;
        
        uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks, "test.bin", fileSize, 1024);
        FileUploadStatus status = uploadStatusService.getUploadStatus(fileId);
        
        // Initially no speed
        assertEquals(0.0, status.getUploadSpeed());
        assertNull(status.getEstimatedRemainingTime());
        
        // Add some chunks with a small delay to simulate upload time
        uploadStatusService.addChunk(fileId, 0, 1024);
        Thread.sleep(10); // Small delay
        uploadStatusService.addChunk(fileId, 1, 1024);
        Thread.sleep(10);
        uploadStatusService.addChunk(fileId, 2, 1024);
        
        // Should have some upload speed now
        assertTrue(status.getUploadSpeed() > 0);
        assertNotNull(status.getEstimatedRemainingTime());
        assertTrue(status.getEstimatedRemainingTime() > 0);
    }

    @Test
    @DisplayName("Should update file metadata correctly")
    void testUpdateFileMetadata() {
        String fileId = "test-metadata-update";
        int totalChunks = 5;
        
        uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
        FileUploadStatus status = uploadStatusService.getUploadStatus(fileId);
        
        // Initially no metadata
        assertNull(status.getFileName());
        assertNull(status.getFileSize());
        assertNull(status.getChunkSize());
        
        // Update metadata
        uploadStatusService.updateFileMetadata(fileId, "updated.pdf", 5120L, 1024);
        
        assertEquals("updated.pdf", status.getFileName());
        assertEquals(5120L, status.getFileSize());
        assertEquals(1024, status.getChunkSize());
    }
}
