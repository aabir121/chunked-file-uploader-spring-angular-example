package com.example.largeupload.controller;

import com.example.largeupload.model.FileUploadStatus;
import com.example.largeupload.model.ResumeUploadResponse;
import com.example.largeupload.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;



import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@DisplayName("Resume Upload Controller Tests")
class ResumeUploadControllerTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private FileUploadController fileUploadController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should resume upload successfully")
    void testResumeUpload() {
        String fileId = "test-resume-123";
        int totalChunks = 10;
        String fileName = "test.pdf";
        Long fileSize = 10240L;
        Integer chunkSize = 1024;

        // Create a mock status with some chunks already uploaded
        FileUploadStatus mockStatus = new FileUploadStatus(fileId, totalChunks, fileName, fileSize, chunkSize);
        mockStatus.addChunk(0, chunkSize);
        mockStatus.addChunk(1, chunkSize);
        mockStatus.addChunk(3, chunkSize);

        when(fileStorageService.getOrCreateUploadStatusWithMetadata(
            eq(fileId), eq(totalChunks), eq(fileName), eq(fileSize), eq(chunkSize)))
            .thenReturn(mockStatus);

        Mono<org.springframework.http.ResponseEntity<ResumeUploadResponse>> result =
            fileUploadController.resumeUpload(fileId, totalChunks, fileName, fileSize, chunkSize);

        StepVerifier.create(result)
            .assertNext(responseEntity -> {
                assertEquals(200, responseEntity.getStatusCode().value());
                ResumeUploadResponse response = responseEntity.getBody();
                assertNotNull(response);
                assertEquals(fileId, response.getFileId());
                assertEquals(totalChunks, response.getTotalChunks());
                assertEquals(fileName, response.getFileName());
                assertEquals(fileSize, response.getFileSize());
                assertEquals(chunkSize, response.getChunkSize());
                assertEquals(3, response.getReceivedChunks().size());
                assertEquals(7, response.getMissingChunks().size());
                assertEquals(2, response.getNextExpectedChunk()); // First missing chunk
                assertTrue(response.isCanResume());
                assertFalse(response.isCompleted());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should resume upload without metadata")
    void testResumeUploadWithoutMetadata() {
        String fileId = "test-resume-simple";
        int totalChunks = 5;

        FileUploadStatus mockStatus = new FileUploadStatus(fileId, totalChunks);
        mockStatus.addChunk(0);
        mockStatus.addChunk(2);

        when(fileStorageService.getUploadStatus(fileId)).thenReturn(null);
        when(fileStorageService.getOrCreateUploadStatus(fileId, totalChunks)).thenReturn(mockStatus);

        Mono<org.springframework.http.ResponseEntity<ResumeUploadResponse>> result =
            fileUploadController.resumeUpload(fileId, totalChunks, null, null, null);

        StepVerifier.create(result)
            .assertNext(responseEntity -> {
                assertEquals(200, responseEntity.getStatusCode().value());
                ResumeUploadResponse response = responseEntity.getBody();
                assertNotNull(response);
                assertEquals(fileId, response.getFileId());
                assertEquals(totalChunks, response.getTotalChunks());
                assertEquals(2, response.getReceivedChunks().size());
                assertEquals(3, response.getMissingChunks().size());
                assertEquals(1, response.getNextExpectedChunk());
                assertTrue(response.isCanResume());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should resume existing upload")
    void testResumeExistingUpload() {
        String fileId = "existing-upload";
        int totalChunks = 8;

        FileUploadStatus existingStatus = new FileUploadStatus(fileId, totalChunks);
        existingStatus.addChunk(0);
        existingStatus.addChunk(1);
        existingStatus.addChunk(2);

        when(fileStorageService.getUploadStatus(fileId)).thenReturn(existingStatus);

        Mono<org.springframework.http.ResponseEntity<ResumeUploadResponse>> result =
            fileUploadController.resumeUpload(fileId, totalChunks, null, null, null);

        StepVerifier.create(result)
            .assertNext(responseEntity -> {
                assertEquals(200, responseEntity.getStatusCode().value());
                ResumeUploadResponse response = responseEntity.getBody();
                assertNotNull(response);
                assertEquals(fileId, response.getFileId());
                assertEquals(totalChunks, response.getTotalChunks());
                assertEquals(3, response.getReceivedChunks().size());
                assertEquals(3, response.getNextExpectedChunk());
                assertTrue(response.isCanResume());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should return error for invalid fileId")
    void testResumeUploadInvalidFileId() {
        Mono<org.springframework.http.ResponseEntity<ResumeUploadResponse>> result =
            fileUploadController.resumeUpload("", 5, null, null, null);

        StepVerifier.create(result)
            .expectError()
            .verify();
    }

    @Test
    @DisplayName("Should return error for invalid totalChunks")
    void testResumeUploadInvalidTotalChunks() {
        Mono<org.springframework.http.ResponseEntity<ResumeUploadResponse>> result =
            fileUploadController.resumeUpload("test-file", 0, null, null, null);

        StepVerifier.create(result)
            .expectError()
            .verify();
    }

    @Test
    @DisplayName("Should get resumable uploads")
    void testGetResumableUploads() {
        FileUploadStatus status1 = new FileUploadStatus("resumable1", 5);
        status1.addChunk(0);
        status1.addChunk(1);

        FileUploadStatus status2 = new FileUploadStatus("resumable2", 3);
        status2.addChunk(0);

        when(fileStorageService.getResumableUploadsReactive())
            .thenReturn(Flux.just(status1, status2));

        Flux<FileUploadStatus> result = fileUploadController.getResumableUploads();

        StepVerifier.create(result)
            .assertNext(status -> assertEquals("resumable1", status.getFileId()))
            .assertNext(status -> assertEquals("resumable2", status.getFileId()))
            .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty list when no resumable uploads")
    void testGetResumableUploadsEmpty() {
        when(fileStorageService.getResumableUploadsReactive())
            .thenReturn(Flux.empty());

        Flux<FileUploadStatus> result = fileUploadController.getResumableUploads();

        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle completed upload in resume")
    void testResumeCompletedUpload() {
        String fileId = "completed-upload";
        int totalChunks = 3;

        FileUploadStatus completedStatus = new FileUploadStatus(fileId, totalChunks);
        completedStatus.addChunk(0);
        completedStatus.addChunk(1);
        completedStatus.addChunk(2);
        completedStatus.markAsCompleted();

        when(fileStorageService.getUploadStatus(fileId)).thenReturn(completedStatus);

        Mono<org.springframework.http.ResponseEntity<ResumeUploadResponse>> result =
            fileUploadController.resumeUpload(fileId, totalChunks, null, null, null);

        StepVerifier.create(result)
            .assertNext(responseEntity -> {
                assertEquals(200, responseEntity.getStatusCode().value());
                ResumeUploadResponse response = responseEntity.getBody();
                assertNotNull(response);
                assertEquals(fileId, response.getFileId());
                assertTrue(response.isCompleted());
                assertFalse(response.isCanResume());
                assertTrue(response.getMissingChunks().isEmpty());
                assertEquals(totalChunks, response.getReceivedChunks().size());
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle failed upload in resume")
    void testResumeFailedUpload() {
        String fileId = "failed-upload";
        int totalChunks = 5;

        FileUploadStatus failedStatus = new FileUploadStatus(fileId, totalChunks);
        failedStatus.addChunk(0);
        failedStatus.markAsFailed("Test failure");

        when(fileStorageService.getUploadStatus(fileId)).thenReturn(failedStatus);

        Mono<org.springframework.http.ResponseEntity<ResumeUploadResponse>> result =
            fileUploadController.resumeUpload(fileId, totalChunks, null, null, null);

        StepVerifier.create(result)
            .assertNext(responseEntity -> {
                assertEquals(200, responseEntity.getStatusCode().value());
                ResumeUploadResponse response = responseEntity.getBody();
                assertNotNull(response);
                assertEquals(fileId, response.getFileId());
                assertTrue(response.isFailed());
                assertFalse(response.isCanResume());
                assertEquals("Test failure", response.getErrorMessage());
            })
            .verifyComplete();
    }
}
