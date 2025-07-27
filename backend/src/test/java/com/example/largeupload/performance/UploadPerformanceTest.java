package com.example.largeupload.performance;

import com.example.largeupload.model.FileUploadStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "PT30S") // 30 second timeout for performance tests
@TestPropertySource(properties = {
    "file-upload.storage.base-directory=perf-test-uploads",
    "file-upload.chunk.max-size=10485760", // 10MB
    "file-upload.performance.max-concurrent-uploads=20"
})
@EnabledIfSystemProperty(named = "run.performance.tests", matches = "true")
class UploadPerformanceTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testLargeFileUpload() throws Exception {
        String fileId = UUID.randomUUID().toString();
        String fileName = "large-file.dat";
        
        // Create a 50MB file in chunks
        int chunkSize = 1024 * 1024; // 1MB chunks
        int totalSize = 50 * 1024 * 1024; // 50MB
        int totalChunks = totalSize / chunkSize;
        
        byte[] chunkData = new byte[chunkSize];
        // Fill with some pattern
        for (int i = 0; i < chunkSize; i++) {
            chunkData[i] = (byte) (i % 256);
        }

        long startTime = System.currentTimeMillis();

        // Upload all chunks
        for (int i = 0; i < totalChunks; i++) {
            uploadChunk(fileId, i, totalChunks, chunkData, fileName);
            
            // Log progress every 10 chunks
            if (i % 10 == 0) {
                System.out.printf("Uploaded chunk %d/%d%n", i + 1, totalChunks);
            }
        }

        long uploadTime = System.currentTimeMillis() - startTime;
        System.out.printf("Upload completed in %d ms%n", uploadTime);

        // Verify upload status
        FileUploadStatus status = getUploadStatus(fileId);
        assertEquals(totalChunks, status.getReceivedChunks().size());
        assertTrue(status.isComplete());

        // Complete upload and measure assembly time
        long assemblyStartTime = System.currentTimeMillis();
        completeUpload(fileId);
        long assemblyTime = System.currentTimeMillis() - assemblyStartTime;
        System.out.printf("File assembly completed in %d ms%n", assemblyTime);

        // Verify final file
        Path uploadDir = Paths.get("perf-test-uploads");
        Path finalFile = uploadDir.resolve(fileName);
        assertTrue(Files.exists(finalFile));
        assertEquals(totalSize, Files.size(finalFile));

        // Performance assertions
        assertTrue(uploadTime < 60000, "Upload should complete within 60 seconds");
        assertTrue(assemblyTime < 10000, "Assembly should complete within 10 seconds");

        // Cleanup
        Files.deleteIfExists(finalFile);
    }

    @Test
    void testConcurrentUploads() throws Exception {
        int numberOfConcurrentUploads = 10;
        int chunksPerFile = 5;
        int chunkSize = 1024; // 1KB chunks for faster testing
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentUploads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Start concurrent uploads
        for (int fileIndex = 0; fileIndex < numberOfConcurrentUploads; fileIndex++) {
            final int fileNum = fileIndex;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String fileId = UUID.randomUUID().toString();
                    String fileName = "concurrent-" + fileNum + ".dat";
                    
                    byte[] chunkData = new byte[chunkSize];
                    // Fill with file-specific pattern
                    for (int i = 0; i < chunkSize; i++) {
                        chunkData[i] = (byte) ((fileNum * 100 + i) % 256);
                    }

                    // Upload all chunks for this file
                    for (int chunkIndex = 0; chunkIndex < chunksPerFile; chunkIndex++) {
                        uploadChunk(fileId, chunkIndex, chunksPerFile, chunkData, fileName);
                    }

                    // Complete upload
                    completeUpload(fileId);
                    
                    System.out.printf("Completed upload for file %d%n", fileNum);
                } catch (Exception e) {
                    throw new RuntimeException("Upload failed for file " + fileNum, e);
                }
            }, executor);
            
            futures.add(future);
        }

        // Wait for all uploads to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(120, TimeUnit.SECONDS);
        
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.printf("All %d concurrent uploads completed in %d ms%n", numberOfConcurrentUploads, totalTime);

        // Verify all files were created
        Path uploadDir = Paths.get("perf-test-uploads");
        for (int i = 0; i < numberOfConcurrentUploads; i++) {
            Path file = uploadDir.resolve("concurrent-" + i + ".dat");
            assertTrue(Files.exists(file), "File " + i + " should exist");
            assertEquals(chunksPerFile * chunkSize, Files.size(file));
            
            // Cleanup
            Files.deleteIfExists(file);
        }

        // Performance assertion
        assertTrue(totalTime < 60000, "Concurrent uploads should complete within 60 seconds");
        
        executor.shutdown();
    }

    @Test
    void testMemoryUsageWithManySmallFiles() throws Exception {
        int numberOfFiles = 100;
        int chunkSize = 1024; // 1KB
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        List<String> fileIds = new ArrayList<>();

        // Upload many small files
        for (int i = 0; i < numberOfFiles; i++) {
            String fileId = UUID.randomUUID().toString();
            String fileName = "small-" + i + ".dat";
            fileIds.add(fileId);
            
            byte[] data = ("Small file content " + i).getBytes();
            uploadChunk(fileId, 0, 1, data, fileName);
            
            if (i % 20 == 0) {
                System.out.printf("Created %d small files%n", i + 1);
            }
        }

        long memoryAfterUploads = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = memoryAfterUploads - initialMemory;
        
        System.out.printf("Memory increase after %d uploads: %d bytes (%.2f MB)%n", 
                         numberOfFiles, memoryIncrease, memoryIncrease / (1024.0 * 1024.0));

        // Complete all uploads
        for (int i = 0; i < numberOfFiles; i++) {
            completeUpload(fileIds.get(i));
        }

        // Verify files and cleanup
        Path uploadDir = Paths.get("perf-test-uploads");
        for (int i = 0; i < numberOfFiles; i++) {
            Path file = uploadDir.resolve("small-" + i + ".dat");
            assertTrue(Files.exists(file));
            Files.deleteIfExists(file);
        }

        // Memory should not increase excessively (less than 50MB for 100 small files)
        assertTrue(memoryIncrease < 50 * 1024 * 1024, 
                  "Memory increase should be reasonable: " + memoryIncrease + " bytes");
    }

    @Test
    void testUploadThroughput() throws Exception {
        String fileId = UUID.randomUUID().toString();
        String fileName = "throughput-test.dat";
        
        int chunkSize = 1024 * 1024; // 1MB chunks
        int numberOfChunks = 20; // 20MB total
        
        byte[] chunkData = new byte[chunkSize];
        // Fill with random-ish data
        for (int i = 0; i < chunkSize; i++) {
            chunkData[i] = (byte) (i % 256);
        }

        long startTime = System.nanoTime();
        
        // Upload chunks and measure throughput
        for (int i = 0; i < numberOfChunks; i++) {
            long chunkStartTime = System.nanoTime();
            uploadChunk(fileId, i, numberOfChunks, chunkData, fileName);
            long chunkTime = System.nanoTime() - chunkStartTime;
            
            double chunkThroughputMBps = (chunkSize / (1024.0 * 1024.0)) / (chunkTime / 1_000_000_000.0);
            System.out.printf("Chunk %d throughput: %.2f MB/s%n", i, chunkThroughputMBps);
        }
        
        long totalTime = System.nanoTime() - startTime;
        double totalThroughputMBps = (numberOfChunks * chunkSize / (1024.0 * 1024.0)) / (totalTime / 1_000_000_000.0);
        
        System.out.printf("Overall upload throughput: %.2f MB/s%n", totalThroughputMBps);
        
        // Complete upload
        completeUpload(fileId);
        
        // Cleanup
        Path uploadDir = Paths.get("perf-test-uploads");
        Path finalFile = uploadDir.resolve(fileName);
        Files.deleteIfExists(finalFile);
        
        // Performance assertion - should achieve at least 1 MB/s
        assertTrue(totalThroughputMBps > 1.0, 
                  "Upload throughput should be at least 1 MB/s, was: " + totalThroughputMBps);
    }

    private void uploadChunk(String fileId, int chunkNumber, int totalChunks, byte[] chunkData, String fileName) {
        webTestClient.post()
                .uri("/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(createMultipartBody(fileId, String.valueOf(chunkNumber), String.valueOf(totalChunks), fileName, chunkData))
                .exchange()
                .expectStatus().isOk();
    }

    private FileUploadStatus getUploadStatus(String fileId) {
        return webTestClient.get()
                .uri("/upload/{fileId}", fileId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(FileUploadStatus.class)
                .returnResult()
                .getResponseBody();
    }

    private void completeUpload(String fileId) {
        webTestClient.post()
                .uri("/upload/{fileId}/complete", fileId)
                .exchange()
                .expectStatus().isOk();
    }

    private BodyInserters.MultipartInserter createMultipartBody(String fileId, String chunkNumber, String totalChunks, String fileName, byte[] fileData) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("fileId", fileId);
        builder.part("chunkNumber", chunkNumber);
        builder.part("totalChunks", totalChunks);
        builder.part("fileName", fileName);
        builder.part("file", new ByteArrayResource(fileData)).filename("chunk.dat");
        return BodyInserters.fromMultipartData(builder.build());
    }
}
