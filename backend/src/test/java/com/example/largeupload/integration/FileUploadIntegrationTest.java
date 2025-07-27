package com.example.largeupload.integration;

import com.example.largeupload.model.FileUploadStatus;
import org.junit.jupiter.api.Test;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
    "file-upload.storage.base-directory=test-uploads",
    "file-upload.chunk.max-size=1048576",
    "file-upload.file.max-size=10485760"
})
class FileUploadIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testCompleteUploadWorkflow() throws Exception {
        String fileId = UUID.randomUUID().toString();
        String fileName = "test-integration.txt";
        String fileContent = "This is a test file for integration testing. It contains multiple chunks of data.";
        byte[] fileBytes = fileContent.getBytes();
        
        int chunkSize = 20; // Small chunks for testing
        int totalChunks = (int) Math.ceil((double) fileBytes.length / chunkSize);

        // Upload all chunks
        for (int i = 0; i < totalChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, fileBytes.length);
            byte[] chunkData = new byte[end - start];
            System.arraycopy(fileBytes, start, chunkData, 0, end - start);

            uploadChunk(fileId, i, totalChunks, chunkData, fileName);
        }

        // Verify upload status shows all chunks received
        FileUploadStatus status = getUploadStatus(fileId);
        assertNotNull(status);
        assertEquals(fileId, status.getFileId());
        assertEquals(totalChunks, status.getTotalChunks());
        assertEquals(totalChunks, status.getReceivedChunks().size());
        assertEquals(fileName, status.getFileName());

        // Complete the upload
        completeUpload(fileId);

        // Verify final file was created
        Path uploadDir = Paths.get("test-uploads");
        Path finalFile = uploadDir.resolve(fileName);
        assertTrue(Files.exists(finalFile), "Final file should exist");
        
        String assembledContent = Files.readString(finalFile);
        assertEquals(fileContent, assembledContent, "Assembled file content should match original");

        // Cleanup
        Files.deleteIfExists(finalFile);
    }

    @Test
    void testUploadWithMissingChunks() {
        String fileId = UUID.randomUUID().toString();
        String fileName = "incomplete.txt";
        int totalChunks = 5;

        // Upload only some chunks (missing chunk 2)
        uploadChunk(fileId, 0, totalChunks, "chunk0".getBytes(), fileName);
        uploadChunk(fileId, 1, totalChunks, "chunk1".getBytes(), fileName);
        // Skip chunk 2
        uploadChunk(fileId, 3, totalChunks, "chunk3".getBytes(), fileName);
        uploadChunk(fileId, 4, totalChunks, "chunk4".getBytes(), fileName);

        // Verify upload status shows missing chunks
        FileUploadStatus status = getUploadStatus(fileId);
        assertNotNull(status);
        assertEquals(4, status.getReceivedChunks().size()); // 4 out of 5 chunks
        assertFalse(status.isComplete());

        // Attempting to complete should fail
        webTestClient.post()
                .uri("/upload/{fileId}/complete", fileId)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testConcurrentUploads() throws Exception {
        String fileId1 = UUID.randomUUID().toString();
        String fileId2 = UUID.randomUUID().toString();
        String fileName1 = "concurrent1.txt";
        String fileName2 = "concurrent2.txt";
        
        String content1 = "Content for first concurrent upload";
        String content2 = "Different content for second concurrent upload";
        
        // Upload chunks for both files concurrently
        uploadChunk(fileId1, 0, 1, content1.getBytes(), fileName1);
        uploadChunk(fileId2, 0, 1, content2.getBytes(), fileName2);

        // Verify both uploads are tracked separately
        FileUploadStatus status1 = getUploadStatus(fileId1);
        FileUploadStatus status2 = getUploadStatus(fileId2);
        
        assertNotNull(status1);
        assertNotNull(status2);
        assertEquals(fileName1, status1.getFileName());
        assertEquals(fileName2, status2.getFileName());
        assertTrue(status1.isComplete());
        assertTrue(status2.isComplete());

        // Complete both uploads
        completeUpload(fileId1);
        completeUpload(fileId2);

        // Verify both files were created correctly
        Path uploadDir = Paths.get("test-uploads");
        Path file1 = uploadDir.resolve(fileName1);
        Path file2 = uploadDir.resolve(fileName2);
        
        assertTrue(Files.exists(file1));
        assertTrue(Files.exists(file2));
        assertEquals(content1, Files.readString(file1));
        assertEquals(content2, Files.readString(file2));

        // Cleanup
        Files.deleteIfExists(file1);
        Files.deleteIfExists(file2);
    }

    @Test
    void testBinaryUpload() throws Exception {
        String fileId = UUID.randomUUID().toString();
        String fileName = "binary-test.bin";
        byte[] binaryData = {0x00, 0x01, 0x02, 0x03, (byte) 0xFF, (byte) 0xFE, (byte) 0xFD};

        // Upload using binary endpoint
        webTestClient.post()
                .uri("/upload/binary")
                .header("X-File-Id", fileId)
                .header("X-Chunk-Number", "0")
                .header("X-Total-Chunks", "1")
                .header("X-File-Name", fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(binaryData)
                .exchange()
                .expectStatus().isOk();

        // Complete upload
        completeUpload(fileId);

        // Verify binary file
        Path uploadDir = Paths.get("test-uploads");
        Path binaryFile = uploadDir.resolve(fileName);
        assertTrue(Files.exists(binaryFile));
        
        byte[] assembledData = Files.readAllBytes(binaryFile);
        assertArrayEquals(binaryData, assembledData);

        // Cleanup
        Files.deleteIfExists(binaryFile);
    }

    @Test
    void testUploadValidation() {
        String fileId = UUID.randomUUID().toString();

        // Test invalid chunk number
        webTestClient.post()
                .uri("/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(createMultipartBody(fileId, "invalid", "5", "test.txt", "data".getBytes()))
                .exchange()
                .expectStatus().isBadRequest();

        // Test missing file ID
        webTestClient.post()
                .uri("/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(createMultipartBody("", "0", "5", "test.txt", "data".getBytes()))
                .exchange()
                .expectStatus().isBadRequest();

        // Test chunk number greater than total chunks
        webTestClient.post()
                .uri("/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(createMultipartBody(fileId, "5", "5", "test.txt", "data".getBytes()))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testGetAllUploads() {
        String fileId1 = UUID.randomUUID().toString();
        String fileId2 = UUID.randomUUID().toString();

        // Create some uploads
        uploadChunk(fileId1, 0, 2, "chunk1".getBytes(), "file1.txt");
        uploadChunk(fileId2, 0, 1, "chunk2".getBytes(), "file2.txt");

        // Get all uploads
        webTestClient.get()
                .uri("/upload")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(FileUploadStatus.class)
                .hasSize(2);
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
