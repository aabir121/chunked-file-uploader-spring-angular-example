package com.example.largeupload.service;

import com.example.largeupload.model.FileUploadStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UploadStatusServiceTest {

    private UploadStatusService uploadStatusService;

    @BeforeEach
    void setUp() {
        uploadStatusService = new UploadStatusService();
    }

    @Test
    void testGetOrCreateUploadStatus() {
        String fileId = "test-file";
        int totalChunks = 5;

        // First call should create new status
        FileUploadStatus status1 = uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
        assertNotNull(status1);
        assertEquals(fileId, status1.getFileId());
        assertEquals(totalChunks, status1.getTotalChunks());

        // Second call should return same status
        FileUploadStatus status2 = uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
        assertSame(status1, status2);
    }

    @Test
    void testGetUploadStatus() {
        String fileId = "test-file";
        int totalChunks = 3;

        // Initially should return null
        assertNull(uploadStatusService.getUploadStatus(fileId));

        // Create status
        uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);

        // Now should return the status
        FileUploadStatus status = uploadStatusService.getUploadStatus(fileId);
        assertNotNull(status);
        assertEquals(fileId, status.getFileId());
    }

    @Test
    void testGetUploadStatusReactive() {
        String fileId = "test-reactive";
        int totalChunks = 3;

        // Initially should be empty
        StepVerifier.create(uploadStatusService.getUploadStatusReactive(fileId))
                .verifyComplete();

        // Create status
        uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);

        // Now should return the status
        StepVerifier.create(uploadStatusService.getUploadStatusReactive(fileId))
                .expectNextMatches(status -> status.getFileId().equals(fileId))
                .verifyComplete();
    }

    @Test
    void testAddChunk() {
        String fileId = "test-chunks";
        int totalChunks = 3;

        FileUploadStatus status = uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
        assertEquals(0, status.getReceivedChunks().size());

        uploadStatusService.addChunk(fileId, 0);
        assertEquals(1, status.getReceivedChunks().size());
        assertTrue(status.getReceivedChunks().contains(0));

        uploadStatusService.addChunk(fileId, 2);
        assertEquals(2, status.getReceivedChunks().size());
        assertTrue(status.getReceivedChunks().contains(2));
    }

    @Test
    void testAddChunkToNonExistentUpload() {
        // Should not throw exception, just log warning
        assertDoesNotThrow(() -> uploadStatusService.addChunk("non-existent", 0));
    }

    @Test
    void testSetFileName() {
        String fileId = "test-filename";
        String fileName = "test.txt";
        int totalChunks = 2;

        FileUploadStatus status = uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
        assertNull(status.getFileName());

        uploadStatusService.setFileName(fileId, fileName);
        assertEquals(fileName, status.getFileName());

        // Setting filename again should not change it
        uploadStatusService.setFileName(fileId, "different.txt");
        assertEquals(fileName, status.getFileName()); // Should still be original
    }

    @Test
    void testMarkAsCompleted() {
        String fileId = "test-completed";
        int totalChunks = 2;

        FileUploadStatus status = uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
        assertFalse(status.isComplete());

        uploadStatusService.markAsCompleted(fileId);
        assertTrue(status.isComplete());
    }

    @Test
    void testMarkAsFailed() {
        String fileId = "test-failed";
        String errorMessage = "Test error";
        int totalChunks = 2;

        FileUploadStatus status = uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
        assertFalse(status.isFailed());
        assertNull(status.getErrorMessage());

        uploadStatusService.markAsFailed(fileId, errorMessage);
        assertTrue(status.isFailed());
        assertEquals(errorMessage, status.getErrorMessage());
    }

    @Test
    void testIsUploadComplete() {
        String fileId = "test-complete-check";
        int totalChunks = 3;

        // Initially not complete
        assertFalse(uploadStatusService.isUploadComplete(fileId));

        FileUploadStatus status = uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
        assertFalse(uploadStatusService.isUploadComplete(fileId));

        // Add all chunks
        uploadStatusService.addChunk(fileId, 0);
        uploadStatusService.addChunk(fileId, 1);
        uploadStatusService.addChunk(fileId, 2);

        assertTrue(uploadStatusService.isUploadComplete(fileId));
    }

    @Test
    void testGetUploadProgress() {
        String fileId = "test-progress";
        int totalChunks = 4;

        // Non-existent upload should return 0
        assertEquals(0.0, uploadStatusService.getUploadProgress(fileId));

        uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
        assertEquals(0.0, uploadStatusService.getUploadProgress(fileId));

        uploadStatusService.addChunk(fileId, 0);
        assertEquals(25.0, uploadStatusService.getUploadProgress(fileId));

        uploadStatusService.addChunk(fileId, 1);
        assertEquals(50.0, uploadStatusService.getUploadProgress(fileId));

        uploadStatusService.addChunk(fileId, 2);
        uploadStatusService.addChunk(fileId, 3);
        assertEquals(100.0, uploadStatusService.getUploadProgress(fileId));
    }

    @Test
    void testGetMissingChunks() {
        String fileId = "test-missing";
        int totalChunks = 5;

        // Non-existent upload should return empty array
        assertEquals(0, uploadStatusService.getMissingChunks(fileId).length);

        uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
        
        // All chunks missing initially
        int[] missing = uploadStatusService.getMissingChunks(fileId);
        assertEquals(5, missing.length);

        // Add some chunks
        uploadStatusService.addChunk(fileId, 0);
        uploadStatusService.addChunk(fileId, 2);
        uploadStatusService.addChunk(fileId, 4);

        missing = uploadStatusService.getMissingChunks(fileId);
        assertEquals(2, missing.length);
        assertArrayEquals(new int[]{1, 3}, missing);
    }

    @Test
    void testGetReceivedChunkCount() {
        String fileId = "test-count";
        int totalChunks = 3;

        assertEquals(0, uploadStatusService.getReceivedChunkCount(fileId));

        uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
        assertEquals(0, uploadStatusService.getReceivedChunkCount(fileId));

        uploadStatusService.addChunk(fileId, 0);
        assertEquals(1, uploadStatusService.getReceivedChunkCount(fileId));

        uploadStatusService.addChunk(fileId, 2);
        assertEquals(2, uploadStatusService.getReceivedChunkCount(fileId));
    }

    @Test
    void testHasChunk() {
        String fileId = "test-has-chunk";
        int totalChunks = 3;

        assertFalse(uploadStatusService.hasChunk(fileId, 0));

        uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
        assertFalse(uploadStatusService.hasChunk(fileId, 0));

        uploadStatusService.addChunk(fileId, 0);
        assertTrue(uploadStatusService.hasChunk(fileId, 0));
        assertFalse(uploadStatusService.hasChunk(fileId, 1));
    }

    @Test
    void testGetAllUploadStatuses() {
        assertTrue(uploadStatusService.getAllUploadStatuses().isEmpty());

        uploadStatusService.getOrCreateUploadStatus("file1", 3);
        uploadStatusService.getOrCreateUploadStatus("file2", 5);

        Collection<FileUploadStatus> statuses = uploadStatusService.getAllUploadStatuses();
        assertEquals(2, statuses.size());
    }

    @Test
    void testGetAllUploadStatusesReactive() {
        StepVerifier.create(uploadStatusService.getAllUploadStatusesReactive())
                .verifyComplete();

        uploadStatusService.getOrCreateUploadStatus("file1", 3);
        uploadStatusService.getOrCreateUploadStatus("file2", 5);

        StepVerifier.create(uploadStatusService.getAllUploadStatusesReactive())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void testRemoveUploadStatus() {
        String fileId = "test-remove";
        int totalChunks = 2;

        uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks);
        assertNotNull(uploadStatusService.getUploadStatus(fileId));

        uploadStatusService.removeUploadStatus(fileId);
        assertNull(uploadStatusService.getUploadStatus(fileId));
    }

    @Test
    void testGetUploadStatistics() {
        UploadStatusService.UploadStatistics stats = uploadStatusService.getUploadStatistics();
        assertEquals(0, stats.getTotalUploads());
        assertEquals(0, stats.getCompletedUploads());
        assertEquals(0, stats.getFailedUploads());
        assertEquals(0, stats.getInProgressUploads());

        // Create some uploads with different states
        uploadStatusService.getOrCreateUploadStatus("file1", 2);
        uploadStatusService.getOrCreateUploadStatus("file2", 3);
        uploadStatusService.getOrCreateUploadStatus("file3", 1);

        uploadStatusService.markAsCompleted("file1");
        uploadStatusService.markAsFailed("file2", "Test error");
        // file3 remains in progress

        stats = uploadStatusService.getUploadStatistics();
        assertEquals(3, stats.getTotalUploads());
        assertEquals(1, stats.getCompletedUploads());
        assertEquals(1, stats.getFailedUploads());
        assertEquals(1, stats.getInProgressUploads());
    }

    @Test
    void testCleanupOldUploads() {
        String fileId1 = "old-completed";
        String fileId2 = "old-failed";
        String fileId3 = "recent-in-progress";

        uploadStatusService.getOrCreateUploadStatus(fileId1, 2);
        uploadStatusService.getOrCreateUploadStatus(fileId2, 2);
        uploadStatusService.getOrCreateUploadStatus(fileId3, 2);

        uploadStatusService.markAsCompleted(fileId1);
        uploadStatusService.markAsFailed(fileId2, "Test error");

        // Cleanup with very short max age (should remove completed and failed)
        int removedCount = uploadStatusService.cleanupOldUploads(0);
        assertEquals(2, removedCount);

        // Only in-progress upload should remain
        assertEquals(1, uploadStatusService.getAllUploadStatuses().size());
        assertNotNull(uploadStatusService.getUploadStatus(fileId3));
    }

    @Test
    void testUpdateTotalChunks() {
        String fileId = "test-update-chunks";
        int originalTotalChunks = 5;
        int newTotalChunks = 7;

        uploadStatusService.getOrCreateUploadStatus(fileId, originalTotalChunks);
        
        // This should log a warning but not change the total chunks
        assertDoesNotThrow(() -> 
            uploadStatusService.updateTotalChunks(fileId, newTotalChunks));
        
        FileUploadStatus status = uploadStatusService.getUploadStatus(fileId);
        assertEquals(originalTotalChunks, status.getTotalChunks()); // Should remain unchanged
    }
}
