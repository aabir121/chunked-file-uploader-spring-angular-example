package com.example.largeupload.service;

import com.example.largeupload.config.FileUploadProperties;
import com.example.largeupload.exception.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileAssemblyServiceTest {

    private FileAssemblyService fileAssemblyService;
    
    @Mock
    private ChunkStorageService chunkStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fileAssemblyService = new FileAssemblyService(chunkStorageService);
    }

    @Test
    void testAssembleFile() throws IOException {
        String fileId = "test-assemble";
        String fileName = "assembled.txt";
        int totalChunks = 3;

        // Create test chunk files
        Path chunk0 = tempDir.resolve("chunk0.tmp");
        Path chunk1 = tempDir.resolve("chunk1.tmp");
        Path chunk2 = tempDir.resolve("chunk2.tmp");
        
        Files.write(chunk0, "Hello ".getBytes());
        Files.write(chunk1, "World ".getBytes());
        Files.write(chunk2, "!".getBytes());

        Path[] chunkFiles = {chunk0, chunk1, chunk2};

        // Mock chunk storage service
        when(chunkStorageService.allChunksExist(fileId, totalChunks)).thenReturn(true);
        when(chunkStorageService.getChunkFiles(fileId, totalChunks)).thenReturn(chunkFiles);
        when(chunkStorageService.getUploadDirectory()).thenReturn(tempDir);

        fileAssemblyService.assembleFile(fileId, totalChunks, fileName);

        // Verify final file was created
        Path finalFile = tempDir.resolve(fileName);
        assertTrue(Files.exists(finalFile));
        
        String content = Files.readString(finalFile);
        assertEquals("Hello World !", content);
    }

    @Test
    void testAssembleFileWithMissingChunks() {
        String fileId = "test-missing";
        String fileName = "missing.txt";
        int totalChunks = 3;

        when(chunkStorageService.allChunksExist(fileId, totalChunks)).thenReturn(false);

        assertThrows(FileStorageException.class, 
            () -> fileAssemblyService.assembleFile(fileId, totalChunks, fileName));
    }

    @Test
    void testAssembleFileReactive() throws IOException {
        String fileId = "test-reactive-assemble";
        String fileName = "reactive.txt";
        int totalChunks = 2;

        // Create test chunk files
        Path chunk0 = tempDir.resolve("chunk0.tmp");
        Path chunk1 = tempDir.resolve("chunk1.tmp");
        
        Files.write(chunk0, "Part1".getBytes());
        Files.write(chunk1, "Part2".getBytes());

        Path[] chunkFiles = {chunk0, chunk1};

        when(chunkStorageService.allChunksExist(fileId, totalChunks)).thenReturn(true);
        when(chunkStorageService.getChunkFiles(fileId, totalChunks)).thenReturn(chunkFiles);
        when(chunkStorageService.getUploadDirectory()).thenReturn(tempDir);

        StepVerifier.create(fileAssemblyService.assembleFileReactive(fileId, totalChunks, fileName))
                .verifyComplete();

        // Verify final file was created
        Path finalFile = tempDir.resolve(fileName);
        assertTrue(Files.exists(finalFile));
        assertEquals("Part1Part2", Files.readString(finalFile));
    }

    @Test
    void testAssembleChunksToFile() throws IOException {
        // Create test chunk files
        Path chunk0 = tempDir.resolve("chunk0.tmp");
        Path chunk1 = tempDir.resolve("chunk1.tmp");
        
        Files.write(chunk0, "First".getBytes());
        Files.write(chunk1, "Second".getBytes());

        Path[] chunkFiles = {chunk0, chunk1};
        Path targetFile = tempDir.resolve("target.txt");

        fileAssemblyService.assembleChunksToFile(chunkFiles, targetFile, "test-file");

        assertTrue(Files.exists(targetFile));
        assertEquals("FirstSecond", Files.readString(targetFile));
    }

    @Test
    void testAssembleChunksToFileWithMissingChunk() throws IOException {
        Path chunk0 = tempDir.resolve("chunk0.tmp");
        Path missingChunk = tempDir.resolve("missing.tmp");
        
        Files.write(chunk0, "First".getBytes());
        // Don't create missingChunk file

        Path[] chunkFiles = {chunk0, missingChunk};
        Path targetFile = tempDir.resolve("target.txt");

        assertThrows(FileStorageException.class, 
            () -> fileAssemblyService.assembleChunksToFile(chunkFiles, targetFile, "test-file"));
    }

    @Test
    void testValidateAssembledFile() throws IOException {
        String fileId = "test-validate";
        int totalChunks = 2;
        Path assembledFile = tempDir.resolve("assembled.txt");
        
        // Create assembled file
        Files.write(assembledFile, "Test content".getBytes());
        
        // Mock chunk storage service to return expected size
        when(chunkStorageService.getTotalChunksSize(fileId, totalChunks))
            .thenReturn((long) "Test content".getBytes().length);

        boolean isValid = fileAssemblyService.validateAssembledFile(fileId, totalChunks, assembledFile);
        assertTrue(isValid);
    }

    @Test
    void testValidateAssembledFileWithSizeMismatch() throws IOException {
        String fileId = "test-validate-mismatch";
        int totalChunks = 2;
        Path assembledFile = tempDir.resolve("assembled.txt");
        
        Files.write(assembledFile, "Test content".getBytes());
        
        // Mock chunk storage service to return different expected size
        when(chunkStorageService.getTotalChunksSize(fileId, totalChunks))
            .thenReturn(100L); // Different from actual file size

        boolean isValid = fileAssemblyService.validateAssembledFile(fileId, totalChunks, assembledFile);
        assertFalse(isValid);
    }

    @Test
    void testValidateAssembledFileNonExistent() {
        String fileId = "test-validate-missing";
        int totalChunks = 2;
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");

        boolean isValid = fileAssemblyService.validateAssembledFile(fileId, totalChunks, nonExistentFile);
        assertFalse(isValid);
    }

    @Test
    void testEstimateFinalFileSize() throws IOException {
        String fileId = "test-estimate";
        int totalChunks = 3;
        long expectedSize = 1024L;

        when(chunkStorageService.getTotalChunksSize(fileId, totalChunks)).thenReturn(expectedSize);

        long estimatedSize = fileAssemblyService.estimateFinalFileSize(fileId, totalChunks);
        assertEquals(expectedSize, estimatedSize);
    }

    @Test
    void testCanAssemble() {
        String fileId = "test-can-assemble";
        int totalChunks = 3;

        when(chunkStorageService.allChunksExist(fileId, totalChunks)).thenReturn(true);
        assertTrue(fileAssemblyService.canAssemble(fileId, totalChunks));

        when(chunkStorageService.allChunksExist(fileId, totalChunks)).thenReturn(false);
        assertFalse(fileAssemblyService.canAssemble(fileId, totalChunks));
    }

    @Test
    void testGetMissingChunks() {
        String fileId = "test-missing-chunks";
        int totalChunks = 5;

        // Mock chunk existence
        when(chunkStorageService.chunkExists(fileId, 0)).thenReturn(true);
        when(chunkStorageService.chunkExists(fileId, 1)).thenReturn(false);
        when(chunkStorageService.chunkExists(fileId, 2)).thenReturn(true);
        when(chunkStorageService.chunkExists(fileId, 3)).thenReturn(false);
        when(chunkStorageService.chunkExists(fileId, 4)).thenReturn(true);

        int[] missingChunks = fileAssemblyService.getMissingChunks(fileId, totalChunks);
        assertArrayEquals(new int[]{1, 3}, missingChunks);
    }

    @Test
    void testAssembleFileWithNamingConflict() throws IOException {
        String fileId = "test-conflict";
        String fileName = "conflict.txt";
        int totalChunks = 1;

        // Create existing file with same name
        Path existingFile = tempDir.resolve(fileName);
        Files.write(existingFile, "existing content".getBytes());

        // Create test chunk file
        Path chunk0 = tempDir.resolve("chunk0.tmp");
        Files.write(chunk0, "new content".getBytes());
        Path[] chunkFiles = {chunk0};

        when(chunkStorageService.allChunksExist(fileId, totalChunks)).thenReturn(true);
        when(chunkStorageService.getChunkFiles(fileId, totalChunks)).thenReturn(chunkFiles);
        when(chunkStorageService.getUploadDirectory()).thenReturn(tempDir);

        fileAssemblyService.assembleFile(fileId, totalChunks, fileName);

        // Should create file with modified name
        Path expectedFile = tempDir.resolve("conflict_1.txt");
        assertTrue(Files.exists(expectedFile));
        assertEquals("new content", Files.readString(expectedFile));
        
        // Original file should still exist
        assertTrue(Files.exists(existingFile));
        assertEquals("existing content", Files.readString(existingFile));
    }

    @Test
    void testAssembleFileWithNullFileName() throws IOException {
        String fileId = "test-null-filename";
        int totalChunks = 1;

        Path chunk0 = tempDir.resolve("chunk0.tmp");
        Files.write(chunk0, "content".getBytes());
        Path[] chunkFiles = {chunk0};

        when(chunkStorageService.allChunksExist(fileId, totalChunks)).thenReturn(true);
        when(chunkStorageService.getChunkFiles(fileId, totalChunks)).thenReturn(chunkFiles);
        when(chunkStorageService.getUploadDirectory()).thenReturn(tempDir);

        fileAssemblyService.assembleFile(fileId, totalChunks, null);

        // Should create file with default name
        Path expectedFile = tempDir.resolve(fileId + ".bin");
        assertTrue(Files.exists(expectedFile));
        assertEquals("content", Files.readString(expectedFile));
    }

    @Test
    void testAssembleFileWithEmptyFileName() throws IOException {
        String fileId = "test-empty-filename";
        int totalChunks = 1;

        Path chunk0 = tempDir.resolve("chunk0.tmp");
        Files.write(chunk0, "content".getBytes());
        Path[] chunkFiles = {chunk0};

        when(chunkStorageService.allChunksExist(fileId, totalChunks)).thenReturn(true);
        when(chunkStorageService.getChunkFiles(fileId, totalChunks)).thenReturn(chunkFiles);
        when(chunkStorageService.getUploadDirectory()).thenReturn(tempDir);

        fileAssemblyService.assembleFile(fileId, totalChunks, "");

        // Should create file with default name
        Path expectedFile = tempDir.resolve(fileId + ".bin");
        assertTrue(Files.exists(expectedFile));
        assertEquals("content", Files.readString(expectedFile));
    }
}
