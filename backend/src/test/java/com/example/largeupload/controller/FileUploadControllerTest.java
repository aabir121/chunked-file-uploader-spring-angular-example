package com.example.largeupload.controller;

import com.example.largeupload.config.FileUploadConfiguration;
import com.example.largeupload.model.FileUploadStatus;
import com.example.largeupload.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;



import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(FileUploadController.class)
@Import(FileUploadConfiguration.class)
class FileUploadControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private FileStorageService fileStorageService;

    @Test
    void testEndpoint_ShouldReturnOk() {
        webTestClient.post()
                .uri("/upload/test")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Test endpoint working");
    }

    @Test
    void getUploadStatus_WhenFileExists_ShouldReturnStatus() {
        // Given
        String fileId = "test-file-id";
        FileUploadStatus status = new FileUploadStatus(fileId, 5);
        status.addChunk(0);
        status.addChunk(1);

        when(fileStorageService.getUploadStatusReactive(fileId))
                .thenReturn(Mono.just(status));

        // When & Then
        webTestClient.get()
                .uri("/upload/{fileId}", fileId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(FileUploadStatus.class)
                .value(result -> {
                    assert result.getFileId().equals(fileId);
                    assert result.getTotalChunks() == 5;
                    assert result.getReceivedChunks().size() == 2;
                });
    }

    @Test
    void getUploadStatus_WhenFileNotExists_ShouldReturnNotFound() {
        // Given
        String fileId = "non-existent-file";
        when(fileStorageService.getUploadStatusReactive(fileId))
                .thenReturn(Mono.empty());

        // When & Then
        webTestClient.get()
                .uri("/upload/{fileId}", fileId)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getAllUploads_ShouldReturnAllStatuses() {
        // Given
        FileUploadStatus status1 = new FileUploadStatus("file1", 3);
        FileUploadStatus status2 = new FileUploadStatus("file2", 5);
        
        when(fileStorageService.getAllUploadStatusesReactive())
                .thenReturn(Flux.just(status1, status2));

        // When & Then
        webTestClient.get()
                .uri("/upload")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(FileUploadStatus.class)
                .hasSize(2);
    }

    @Test
    void uploadChunk_WithValidParameters_ShouldReturnOk() {
        // Given
        when(fileStorageService.saveChunkReactiveEnhanced(anyString(), anyInt(), anyInt(), any(byte[].class), anyString()))
                .thenReturn(Mono.empty());

        // Create proper multipart data
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        String body = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"fileId\"\r\n\r\n" +
                "test-file\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"chunkNumber\"\r\n\r\n" +
                "0\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"totalChunks\"\r\n\r\n" +
                "5\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"fileName\"\r\n\r\n" +
                "test.txt\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n" +
                "Content-Type: text/plain\r\n\r\n" +
                "test-content\r\n" +
                "--" + boundary + "--\r\n";

        // When & Then
        webTestClient.post()
                .uri("/upload")
                .contentType(MediaType.parseMediaType("multipart/form-data; boundary=" + boundary))
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void uploadChunk_WithInvalidFileId_ShouldReturnBadRequest() {
        // Create proper multipart data with empty fileId
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        String body = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"fileId\"\r\n\r\n" +
                "\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"chunkNumber\"\r\n\r\n" +
                "0\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"totalChunks\"\r\n\r\n" +
                "5\r\n" +
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"test.txt\"\r\n" +
                "Content-Type: text/plain\r\n\r\n" +
                "test-content\r\n" +
                "--" + boundary + "--\r\n";

        webTestClient.post()
                .uri("/upload")
                .contentType(MediaType.parseMediaType("multipart/form-data; boundary=" + boundary))
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
