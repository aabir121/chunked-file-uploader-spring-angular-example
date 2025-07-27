package com.example.largeupload.service;

import com.example.largeupload.config.FileUploadProperties;
import com.example.largeupload.exception.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ChunkStorageServiceTest {

    private ChunkStorageService chunkStorageService;
    private FileUploadProperties properties;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        properties = new FileUploadProperties();
        properties.getStorage().setBaseDirectory(tempDir.toString());
        properties.getStorage().setTempDirectoryPrefix("temp_");
        
        chunkStorageService = new ChunkStorageService(properties);
    }

    @Test
    void testSaveChunk() throws IOException {
        String fileId = "test-file-123";
        int chunkNumber = 0;
        byte[] chunkData = "Hello World".getBytes();

        chunkStorageService.saveChunk(fileId, chunkNumber, chunkData);

        // Verify temporary directory was created
        Path tempUploadDir = tempDir.resolve("temp_" + fileId);
        assertTrue(Files.exists(tempUploadDir), "Temporary directory should be created");
        assertTrue(Files.isDirectory(tempUploadDir), "Should be a directory");

        // Verify chunk file was created in temp directory
        Path chunkFile = tempUploadDir.resolve(fileId + ".part" + chunkNumber);
        assertTrue(Files.exists(chunkFile), "Chunk file should exist in temp directory");
        assertArrayEquals(chunkData, Files.readAllBytes(chunkFile), "Chunk data should match");
    }

    @Test
    void testSaveChunkReactive() {
        String fileId = "test-reactive";
        int chunkNumber = 1;
        byte[] chunkData = "Reactive test".getBytes();

        StepVerifier.create(chunkStorageService.saveChunkReactive(fileId, chunkNumber, chunkData))
                .verifyComplete();

        // Verify chunk was saved
        assertTrue(chunkStorageService.chunkExists(fileId, chunkNumber));
    }

    @Test
    void testChunkExists() throws IOException {
        String fileId = "test-exists";
        int chunkNumber = 2;
        byte[] chunkData = "Exists test".getBytes();

        // Initially should not exist
        assertFalse(chunkStorageService.chunkExists(fileId, chunkNumber));

        // Save chunk
        chunkStorageService.saveChunk(fileId, chunkNumber, chunkData);

        // Now should exist
        assertTrue(chunkStorageService.chunkExists(fileId, chunkNumber));
    }

    @Test
    void testGetChunkFiles() throws IOException {
        String fileId = "test-get-chunks";
        int totalChunks = 3;
        byte[] chunkData1 = "Chunk 1".getBytes();
        byte[] chunkData2 = "Chunk 2".getBytes();
        byte[] chunkData3 = "Chunk 3".getBytes();

        // Save all chunks
        chunkStorageService.saveChunk(fileId, 0, chunkData1);
        chunkStorageService.saveChunk(fileId, 1, chunkData2);
        chunkStorageService.saveChunk(fileId, 2, chunkData3);

        Path[] chunkFiles = chunkStorageService.getChunkFiles(fileId, totalChunks);

        assertEquals(totalChunks, chunkFiles.length);
        for (int i = 0; i < totalChunks; i++) {
            assertTrue(Files.exists(chunkFiles[i]), "Chunk file " + i + " should exist");
        }
    }

    @Test
    void testGetChunkFilesWithMissingChunk() throws IOException {
        String fileId = "test-missing-chunk";
        int totalChunks = 3;

        // Save only first two chunks
        chunkStorageService.saveChunk(fileId, 0, "Chunk 1".getBytes());
        chunkStorageService.saveChunk(fileId, 1, "Chunk 2".getBytes());
        // Missing chunk 2

        assertThrows(FileStorageException.class, 
            () -> chunkStorageService.getChunkFiles(fileId, totalChunks));
    }

    @Test
    void testAllChunksExist() throws IOException {
        String fileId = "test-all-chunks";
        int totalChunks = 2;

        // Initially no chunks exist
        assertFalse(chunkStorageService.allChunksExist(fileId, totalChunks));

        // Save first chunk
        chunkStorageService.saveChunk(fileId, 0, "Chunk 1".getBytes());
        assertFalse(chunkStorageService.allChunksExist(fileId, totalChunks));

        // Save second chunk
        chunkStorageService.saveChunk(fileId, 1, "Chunk 2".getBytes());
        assertTrue(chunkStorageService.allChunksExist(fileId, totalChunks));
    }

    @Test
    void testCleanupTempDirectory() throws IOException {
        String fileId = "test-cleanup";
        byte[] chunkData = "Test data".getBytes();

        // Save a chunk to create temp directory
        chunkStorageService.saveChunk(fileId, 0, chunkData);

        Path tempUploadDir = tempDir.resolve("temp_" + fileId);
        assertTrue(Files.exists(tempUploadDir), "Temp directory should exist before cleanup");

        // Cleanup
        chunkStorageService.cleanupTempDirectory(fileId);

        // Verify cleanup
        assertFalse(Files.exists(tempUploadDir), "Temp directory should be removed after cleanup");
    }

    @Test
    void testCleanupTempDirectoryReactive() throws IOException {
        String fileId = "test-cleanup-reactive";
        byte[] chunkData = "Test data".getBytes();

        // Save a chunk to create temp directory
        chunkStorageService.saveChunk(fileId, 0, chunkData);

        Path tempUploadDir = tempDir.resolve("temp_" + fileId);
        assertTrue(Files.exists(tempUploadDir), "Temp directory should exist before cleanup");

        StepVerifier.create(chunkStorageService.cleanupTempDirectoryReactive(fileId))
                .verifyComplete();

        // Verify cleanup
        assertFalse(Files.exists(tempUploadDir), "Temp directory should be removed after cleanup");
    }

    @Test
    void testGetChunkSize() throws IOException {
        String fileId = "test-chunk-size";
        int chunkNumber = 0;
        byte[] chunkData = "Test chunk data".getBytes();

        chunkStorageService.saveChunk(fileId, chunkNumber, chunkData);

        long size = chunkStorageService.getChunkSize(fileId, chunkNumber);
        assertEquals(chunkData.length, size);
    }

    @Test
    void testGetChunkSizeNonExistent() {
        String fileId = "non-existent";
        int chunkNumber = 0;

        assertThrows(FileStorageException.class, 
            () -> chunkStorageService.getChunkSize(fileId, chunkNumber));
    }

    @Test
    void testGetTotalChunksSize() throws IOException {
        String fileId = "test-total-size";
        byte[] chunk1 = "Chunk 1 data".getBytes();
        byte[] chunk2 = "Chunk 2 data longer".getBytes();

        chunkStorageService.saveChunk(fileId, 0, chunk1);
        chunkStorageService.saveChunk(fileId, 1, chunk2);

        long totalSize = chunkStorageService.getTotalChunksSize(fileId, 2);
        assertEquals(chunk1.length + chunk2.length, totalSize);
    }

    @Test
    void testMultipleUploadsUseSeparateTempDirectories() throws IOException {
        String fileId1 = "file-1";
        String fileId2 = "file-2";
        byte[] data1 = "Data for file 1".getBytes();
        byte[] data2 = "Data for file 2".getBytes();

        // Save chunks for both files
        chunkStorageService.saveChunk(fileId1, 0, data1);
        chunkStorageService.saveChunk(fileId2, 0, data2);

        // Verify separate temp directories
        Path tempDir1 = tempDir.resolve("temp_" + fileId1);
        Path tempDir2 = tempDir.resolve("temp_" + fileId2);

        assertTrue(Files.exists(tempDir1), "Temp directory for file 1 should exist");
        assertTrue(Files.exists(tempDir2), "Temp directory for file 2 should exist");

        // Verify chunks are in correct directories
        assertTrue(Files.exists(tempDir1.resolve(fileId1 + ".part0")), "Chunk for file 1 should be in its temp directory");
        assertTrue(Files.exists(tempDir2.resolve(fileId2 + ".part0")), "Chunk for file 2 should be in its temp directory");
    }

    @Test
    void testGetTempDirectory() {
        String fileId = "test-temp-dir";
        Path tempDirectory = chunkStorageService.getTempDirectory(fileId);
        
        assertEquals(tempDir.resolve("temp_" + fileId), tempDirectory);
    }

    @Test
    void testGetUploadDirectory() {
        Path uploadDirectory = chunkStorageService.getUploadDirectory();
        assertEquals(tempDir, uploadDirectory);
    }
}
