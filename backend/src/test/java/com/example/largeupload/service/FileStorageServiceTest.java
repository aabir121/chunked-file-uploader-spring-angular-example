package com.example.largeupload.service;

import com.example.largeupload.model.FileUploadStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @Mock
    private ChunkStorageService chunkStorageService;

    @Mock
    private FileAssemblyService fileAssemblyService;

    @Mock
    private UploadStatusService uploadStatusService;



    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fileStorageService = new FileStorageService(
            chunkStorageService,
            fileAssemblyService,
            uploadStatusService
        );
    }

    @Test
    void testSaveChunk() throws IOException {
        String fileId = "test-file-123";
        String fileName = "test.txt";
        byte[] chunkData = "Hello World".getBytes();
        int chunkNumber = 0;
        int totalChunks = 2;

        // Mock the upload status service
        FileUploadStatus mockStatus = new FileUploadStatus(fileId, totalChunks);
        when(uploadStatusService.getOrCreateUploadStatus(fileId, totalChunks)).thenReturn(mockStatus);

        fileStorageService.saveChunk(fileId, chunkNumber, totalChunks, chunkData, fileName);

        // Verify interactions with dependencies
        verify(uploadStatusService).getOrCreateUploadStatus(fileId, totalChunks);
        verify(uploadStatusService).setFileName(fileId, fileName);
        verify(chunkStorageService).saveChunk(fileId, chunkNumber, chunkData);
        verify(uploadStatusService).addChunk(fileId, chunkNumber);
    }

    @Test
    void testCompleteUpload() throws IOException {
        String fileId = "test-file-456";
        String fileName = "complete-test.txt";
        int totalChunks = 2;

        // Mock upload status
        FileUploadStatus mockStatus = new FileUploadStatus(fileId, totalChunks);
        mockStatus.setFileName(fileName);
        when(uploadStatusService.getUploadStatus(fileId)).thenReturn(mockStatus);
        when(uploadStatusService.isUploadComplete(fileId)).thenReturn(true);

        fileStorageService.completeUpload(fileId);

        // Verify interactions
        verify(uploadStatusService).getUploadStatus(fileId);
        verify(uploadStatusService).isUploadComplete(fileId);
        verify(fileAssemblyService).assembleFile(fileId, totalChunks, fileName);
        verify(uploadStatusService).markAsCompleted(fileId);
        verify(chunkStorageService).cleanupTempDirectory(fileId);
    }

    @Test
    void testCompleteUploadWithMissingChunks() {
        String fileId = "test-file-incomplete";
        int totalChunks = 3;

        // Mock upload status with missing chunks
        FileUploadStatus mockStatus = new FileUploadStatus(fileId, totalChunks);
        when(uploadStatusService.getUploadStatus(fileId)).thenReturn(mockStatus);
        when(uploadStatusService.isUploadComplete(fileId)).thenReturn(false);
        when(uploadStatusService.getMissingChunks(fileId)).thenReturn(new int[]{1, 2});

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> fileStorageService.completeUpload(fileId));

        assertTrue(exception.getMessage().contains("Upload is not complete"));
        verify(uploadStatusService, never()).markAsCompleted(fileId);
    }

    @Test
    void testCleanupTempDirectory() {
        String fileId = "test-cleanup-789";

        fileStorageService.cleanupTempDirectory(fileId);

        // Verify interactions
        verify(chunkStorageService).cleanupTempDirectory(fileId);
        verify(uploadStatusService).removeUploadStatus(fileId);
    }

    @Test
    void testGetUploadStatus() {
        String fileId = "test-status";
        FileUploadStatus mockStatus = new FileUploadStatus(fileId, 5);
        when(uploadStatusService.getUploadStatus(fileId)).thenReturn(mockStatus);

        FileUploadStatus result = fileStorageService.getUploadStatus(fileId);

        assertEquals(mockStatus, result);
        verify(uploadStatusService).getUploadStatus(fileId);
    }

    @Test
    void testGetUploadProgress() {
        String fileId = "test-progress";
        double expectedProgress = 75.0;
        when(uploadStatusService.getUploadProgress(fileId)).thenReturn(expectedProgress);

        double result = fileStorageService.getUploadProgress(fileId);

        assertEquals(expectedProgress, result);
        verify(uploadStatusService).getUploadProgress(fileId);
    }

    @Test
    void testCanCompleteUpload() {
        String fileId = "test-can-complete";
        int totalChunks = 3;
        FileUploadStatus mockStatus = new FileUploadStatus(fileId, totalChunks);

        when(uploadStatusService.getUploadStatus(fileId)).thenReturn(mockStatus);
        when(uploadStatusService.isUploadComplete(fileId)).thenReturn(true);
        when(fileAssemblyService.canAssemble(fileId, totalChunks)).thenReturn(true);

        boolean result = fileStorageService.canCompleteUpload(fileId);

        assertTrue(result);
        verify(uploadStatusService).getUploadStatus(fileId);
        verify(uploadStatusService).isUploadComplete(fileId);
        verify(fileAssemblyService).canAssemble(fileId, totalChunks);
    }
}
