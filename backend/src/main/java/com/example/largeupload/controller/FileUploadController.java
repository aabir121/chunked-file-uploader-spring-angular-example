package com.example.largeupload.controller;

import com.example.largeupload.model.FileUploadStatus;
import com.example.largeupload.model.ResumeUploadResponse;
import com.example.largeupload.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import com.example.largeupload.exception.ValidationException;

import java.util.Collection;

@RestController
@RequestMapping("/upload")
@Tag(name = "File Upload", description = "API for uploading large files in chunks")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);
    private final FileStorageService fileStorageService;

    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/test")
    public Mono<ResponseEntity<String>> testEndpoint() {
        logger.info("Test endpoint reached!");
        return Mono.just(ResponseEntity.ok("Test endpoint working"));
    }

    @Operation(summary = "Upload a file chunk",
            description = "Uploads a single chunk of a large file. The file is identified by a unique fileId.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Chunk uploaded successfully"),
                    @ApiResponse(responseCode = "400", description = "Bad request - invalid parameters"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            })
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Void>> uploadChunk(
            @Parameter(description = "The file chunk") @RequestPart("file") FilePart file,
            @Parameter(description = "Unique identifier for the file") @RequestPart("fileId") String fileId,
            @Parameter(description = "The chunk number (0-based)") @RequestPart("chunkNumber") String chunkNumber,
            @Parameter(description = "Total number of chunks for the file") @RequestPart("totalChunks") String totalChunks,
            @Parameter(description = "Original filename") @RequestPart("fileName") String fileName) {

        logger.info("Received upload request - fileId: {}, chunkNumber: {}, totalChunks: {}, fileName: {}, chunkFile: {}",
                   fileId, chunkNumber, totalChunks, fileName,
                   (file != null ? file.filename() : "null"));

        // Validation - let the service handle detailed validation
        if (fileId == null || fileId.trim().isEmpty()) {
            return Mono.error(new ValidationException("fileId is required", "fileId", fileId));
        }
        if (chunkNumber == null || chunkNumber.trim().isEmpty()) {
            return Mono.error(new ValidationException("chunkNumber is required", "chunkNumber", chunkNumber));
        }
        if (totalChunks == null || totalChunks.trim().isEmpty()) {
            return Mono.error(new ValidationException("totalChunks is required", "totalChunks", totalChunks));
        }
        if (file == null) {
            return Mono.error(new ValidationException("file is required", "file", null));
        }

        return filePartToBytes(file)
                .flatMap(fileBytes -> processChunk(fileId, chunkNumber, totalChunks, fileBytes, fileName))
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .doOnSuccess(response -> logger.info("Chunk saved successfully for fileId: {}, chunk: {}", 
                                                   fileId, chunkNumber));
    }

    @Operation(summary = "Upload a file chunk (Fully Reactive)",
            description = "Uploads a single chunk of a large file using reactive streams. This is the preferred method for WebFlux.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Chunk uploaded successfully"),
                    @ApiResponse(responseCode = "400", description = "Bad request - invalid parameters"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            })
    @PostMapping(value = "/reactive", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Void>> uploadChunkReactive(
            @Parameter(description = "The file chunk") @RequestPart("file") Mono<Part> filePart,
            @Parameter(description = "Unique identifier for the file") @RequestPart("fileId") String fileId,
            @Parameter(description = "The chunk number (0-based)") @RequestPart("chunkNumber") String chunkNumber,
            @Parameter(description = "Total number of chunks for the file") @RequestPart("totalChunks") String totalChunks) {

        logger.info("Received reactive upload request - fileId: {}, chunkNumber: {}, totalChunks: {}",
                   fileId, chunkNumber, totalChunks);

        // Validation
        if (fileId == null || fileId.trim().isEmpty()) {
            return Mono.error(new ValidationException("fileId is required", "fileId", fileId));
        }
        if (chunkNumber == null || chunkNumber.trim().isEmpty()) {
            return Mono.error(new ValidationException("chunkNumber is required", "chunkNumber", chunkNumber));
        }
        if (totalChunks == null || totalChunks.trim().isEmpty()) {
            return Mono.error(new ValidationException("totalChunks is required", "totalChunks", totalChunks));
        }

        return filePart
                .flatMap(part -> {
                    // Read the part content as bytes
                    return part.content()
                            .reduce(DataBuffer::write)
                            .map(dataBuffer -> {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                DataBufferUtils.release(dataBuffer);
                                return bytes;
                            });
                })
                .flatMap(fileBytes -> {
                    try {
                        int chunkNum = Integer.parseInt(chunkNumber);
                        int totalChunksNum = Integer.parseInt(totalChunks);
                        return fileStorageService.saveChunkReactiveEnhanced(fileId, chunkNum, totalChunksNum, fileBytes);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid number format in reactive upload for fileId: {}, chunkNumber: {}, totalChunks: {}",
                                   fileId, chunkNumber, totalChunks, e);
                        return Mono.error(new ValidationException("Invalid number format: " + e.getMessage(),
                                                                 "chunkNumber/totalChunks", chunkNumber + "/" + totalChunks));
                    }
                })
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .doOnSuccess(response -> logger.info("Reactive chunk saved successfully for fileId: {}, chunk: {}", fileId, chunkNumber));
    }

    @Operation(summary = "Get upload status",
            description = "Retrieves the upload status for a specific file, including the list of received chunks.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = FileUploadStatus.class))),
                    @ApiResponse(responseCode = "404", description = "File not found")
            })
    @GetMapping("/{fileId}")
    public Mono<ResponseEntity<FileUploadStatus>> getUploadStatus(
            @Parameter(description = "Unique identifier for the file") @PathVariable String fileId) {
        return fileStorageService.getUploadStatusReactive(fileId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Upload a file chunk (Binary)",
            description = "Uploads a single chunk as raw binary data. More efficient for large chunks.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Chunk uploaded successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
            })
    @PostMapping(value = "/binary", consumes = "application/octet-stream")
    public Mono<ResponseEntity<Void>> uploadChunkBinary(
            @Parameter(description = "Unique identifier for the file") @RequestHeader("X-File-Id") String fileId,
            @Parameter(description = "The chunk number (0-based)") @RequestHeader("X-Chunk-Number") String chunkNumber,
            @Parameter(description = "Total number of chunks for the file") @RequestHeader("X-Total-Chunks") String totalChunks,
            @Parameter(description = "Original filename") @RequestHeader("X-File-Name") String fileName,
            @RequestBody Mono<DataBuffer> body) {

        logger.info("Received binary upload request - fileId: {}, chunkNumber: {}, totalChunks: {}, fileName: {}",
                   fileId, chunkNumber, totalChunks, fileName);

        // Validation
        if (fileId == null || fileId.trim().isEmpty()) {
            return Mono.error(new ValidationException("fileId is required", "fileId", fileId));
        }
        if (chunkNumber == null || chunkNumber.trim().isEmpty()) {
            return Mono.error(new ValidationException("chunkNumber is required", "chunkNumber", chunkNumber));
        }
        if (totalChunks == null || totalChunks.trim().isEmpty()) {
            return Mono.error(new ValidationException("totalChunks is required", "totalChunks", totalChunks));
        }

        return body
                .map(this::dataBufferToBytes)
                .flatMap(fileBytes -> processChunk(fileId, chunkNumber, totalChunks, fileBytes, fileName))
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .doOnSuccess(response -> logger.info("Binary chunk saved successfully for fileId: {}, chunk: {}", fileId, chunkNumber));
    }

    @Operation(summary = "Complete file upload",
            description = "Manually triggers file combination after all chunks have been uploaded. This should be called after all chunks are successfully uploaded.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "File successfully combined and upload completed"),
                    @ApiResponse(responseCode = "400", description = "Upload is not ready for completion (missing chunks)"),
                    @ApiResponse(responseCode = "404", description = "File upload not found"),
                    @ApiResponse(responseCode = "500", description = "Error during file combination")
            })
    @PostMapping("/{fileId}/complete")
    public Mono<ResponseEntity<Void>> completeUpload(
            @Parameter(description = "Unique identifier for the file") @PathVariable String fileId) {

        logger.info("Received complete upload request for fileId: {}", fileId);

        return fileStorageService.completeUploadReactive(fileId)
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .doOnSuccess(response -> logger.info("Upload completed successfully for fileId: {}", fileId));
    }

    @Operation(summary = "Cancel upload",
            description = "Cancels an ongoing file upload and cleans up temporary files.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Upload cancelled successfully"),
                    @ApiResponse(responseCode = "404", description = "Upload not found")
            })
    @DeleteMapping("/{fileId}")
    public Mono<ResponseEntity<Void>> cancelUpload(
            @Parameter(description = "Unique identifier for the file") @PathVariable String fileId) {

        logger.info("Received cancel upload request for fileId: {}", fileId);

        return Mono.fromRunnable(() -> fileStorageService.cleanupTempDirectory(fileId))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(ResponseEntity.ok().<Void>build()))
                .doOnSuccess(response -> logger.info("Upload cancelled successfully for fileId: {}", fileId));
    }

    @Operation(summary = "Get all uploads",
            description = "Retrieves the status of all ongoing file uploads.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Statuses retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Collection.class)))
            })
    @GetMapping
    public Flux<FileUploadStatus> getAllUploads() {
        return fileStorageService.getAllUploadStatusesReactive();
    }

    @Operation(summary = "Resume upload",
            description = "Initializes or resumes an upload session. Returns the current status and missing chunks for resume functionality.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Upload session initialized/resumed successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ResumeUploadResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
            })
    @PostMapping("/{fileId}/resume")
    public Mono<ResponseEntity<ResumeUploadResponse>> resumeUpload(
            @Parameter(description = "Unique identifier for the file") @PathVariable String fileId,
            @Parameter(description = "Total number of chunks for the file") @RequestParam int totalChunks,
            @Parameter(description = "Original filename") @RequestParam(required = false) String fileName,
            @Parameter(description = "Total file size in bytes") @RequestParam(required = false) Long fileSize,
            @Parameter(description = "Chunk size in bytes") @RequestParam(required = false) Integer chunkSize) {

        logger.info("Resume upload request - fileId: {}, totalChunks: {}, fileName: {}, fileSize: {}, chunkSize: {}",
                   fileId, totalChunks, fileName, fileSize, chunkSize);

        // Validate parameters
        if (fileId == null || fileId.trim().isEmpty()) {
            return Mono.error(new ValidationException("fileId is required", "fileId", null));
        }
        if (totalChunks <= 0) {
            return Mono.error(new ValidationException("totalChunks must be positive", "totalChunks", String.valueOf(totalChunks)));
        }

        return Mono.fromCallable(() -> {
            // Get or create upload status with enhanced metadata
            FileUploadStatus status;
            if (fileName != null || fileSize != null || chunkSize != null) {
                status = fileStorageService.getOrCreateUploadStatusWithMetadata(fileId, totalChunks, fileName, fileSize, chunkSize);
            } else {
                status = fileStorageService.getUploadStatus(fileId);
                if (status == null) {
                    status = fileStorageService.getOrCreateUploadStatus(fileId, totalChunks);
                }
            }

            // Create response with resume information using constructor
            ResumeUploadResponse response = new ResumeUploadResponse(status);
            return ResponseEntity.ok(response);
        })
        .doOnSuccess(response -> logger.info("Resume upload response created for fileId: {}", fileId))
        .doOnError(error -> logger.error("Failed to create resume upload response for fileId: {}", fileId, error));
    }

    @Operation(summary = "Get resumable uploads",
            description = "Retrieves all uploads that can be resumed (not completed and not failed).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Resumable uploads retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = Collection.class)))
            })
    @GetMapping("/resumable")
    public Flux<FileUploadStatus> getResumableUploads() {
        return fileStorageService.getResumableUploadsReactive();
    }

    /**
     * Helper method to process chunk data and save it
     */
    private Mono<Void> processChunk(String fileId, String chunkNumber, String totalChunks, byte[] chunkData, String fileName) {
        try {
            int chunkNum = Integer.parseInt(chunkNumber);
            int totalChunksNum = Integer.parseInt(totalChunks);
            return fileStorageService.saveChunkReactiveEnhanced(fileId, chunkNum, totalChunksNum, chunkData, fileName);
        } catch (NumberFormatException e) {
            logger.warn("Invalid number format for fileId: {}, chunkNumber: {}, totalChunks: {}",
                       fileId, chunkNumber, totalChunks, e);
            return Mono.error(new ValidationException("Invalid number format: " + e.getMessage(),
                                                     "chunkNumber/totalChunks", chunkNumber + "/" + totalChunks));
        }
    }

    /**
     * Helper method to convert FilePart to byte array
     */
    private Mono<byte[]> filePartToBytes(FilePart filePart) {
        return filePart.content()
                .reduce(DataBuffer::write)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return bytes;
                });
    }

    /**
     * Helper method to convert DataBuffer to byte array
     */
    private byte[] dataBufferToBytes(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);
        return bytes;
    }
}
