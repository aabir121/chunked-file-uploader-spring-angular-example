package com.example.largeupload.service;

import com.example.largeupload.config.FileUploadProperties;
import com.example.largeupload.exception.FileStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Service responsible for storing and managing file chunks
 */
@Service
public class ChunkStorageService {

    private static final Logger logger = LoggerFactory.getLogger(ChunkStorageService.class);
    
    private final Path uploadDir;
    private final String tempDirPrefix;

    public ChunkStorageService(FileUploadProperties properties) {
        this.uploadDir = Paths.get(properties.getStorage().getBaseDirectory());
        this.tempDirPrefix = properties.getStorage().getTempDirectoryPrefix();
        initializeUploadDirectory();
    }

    private void initializeUploadDirectory() {
        try {
            Files.createDirectories(uploadDir);
            logger.info("Upload directory created/verified at: {}", uploadDir.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create upload directory at: {}", uploadDir.toAbsolutePath(), e);
            throw new FileStorageException("Could not create upload directory", null, "INIT", e);
        }
    }

    /**
     * Saves a chunk to the temporary directory for the given file ID
     */
    public void saveChunk(String fileId, int chunkNumber, byte[] chunkData) throws IOException {
        logger.debug("Saving chunk {} for fileId: {}, chunk size: {} bytes", 
                    chunkNumber, fileId, chunkData.length);

        Path tempUploadDir = getTempDirectory(fileId);
        createTempDirectoryIfNotExists(tempUploadDir, fileId);

        Path chunkPath = tempUploadDir.resolve(fileId + ".part" + chunkNumber);
        try {
            Files.write(chunkPath, chunkData, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            logger.debug("Successfully saved chunk {} for fileId: {} at path: {}", 
                        chunkNumber, fileId, chunkPath);
        } catch (IOException e) {
            logger.error("Failed to save chunk {} for fileId: {} at path: {}", 
                        chunkNumber, fileId, chunkPath, e);
            throw new FileStorageException("Failed to save chunk", fileId, "SAVE_CHUNK", e);
        }
    }

    /**
     * Reactive version of saveChunk
     */
    public Mono<Void> saveChunkReactive(String fileId, int chunkNumber, byte[] chunkData) {
        return Mono.fromCallable(() -> {
            try {
                saveChunk(fileId, chunkNumber, chunkData);
                return null;
            } catch (IOException e) {
                logger.error("Reactive save chunk failed for fileId: {}, chunk: {}", fileId, chunkNumber, e);
                throw new FileStorageException("Failed to save chunk reactively", fileId, "SAVE_CHUNK_REACTIVE", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    /**
     * Checks if a specific chunk exists for the given file ID
     */
    public boolean chunkExists(String fileId, int chunkNumber) {
        Path tempUploadDir = getTempDirectory(fileId);
        Path chunkPath = tempUploadDir.resolve(fileId + ".part" + chunkNumber);
        return Files.exists(chunkPath);
    }

    /**
     * Gets the path to a specific chunk file
     */
    public Path getChunkPath(String fileId, int chunkNumber) {
        Path tempUploadDir = getTempDirectory(fileId);
        return tempUploadDir.resolve(fileId + ".part" + chunkNumber);
    }

    /**
     * Gets all chunk files for a given file ID
     */
    public Path[] getChunkFiles(String fileId, int totalChunks) throws IOException {
        Path tempUploadDir = getTempDirectory(fileId);
        Path[] chunkFiles = new Path[totalChunks];
        
        for (int i = 0; i < totalChunks; i++) {
            Path chunkPath = tempUploadDir.resolve(fileId + ".part" + i);
            if (!Files.exists(chunkPath)) {
                throw new FileStorageException(
                    "Missing chunk file: " + i,
                    fileId,
                    "GET_CHUNKS"
                );
            }
            chunkFiles[i] = chunkPath;
        }
        
        return chunkFiles;
    }

    /**
     * Cleans up the temporary directory for a specific file ID
     */
    public void cleanupTempDirectory(String fileId) {
        Path tempUploadDir = getTempDirectory(fileId);
        try {
            if (Files.exists(tempUploadDir)) {
                try (var pathStream = Files.walk(tempUploadDir)) {
                    pathStream.sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                             .forEach(path -> {
                                 try {
                                     Files.delete(path);
                                     logger.debug("Deleted temp file/directory: {}", path);
                                 } catch (IOException ex) {
                                     logger.warn("Failed to delete temp file during cleanup: {}", path, ex);
                                 }
                             });
                }
                logger.info("Cleaned up temporary directory for fileId: {}", fileId);
            }
        } catch (IOException e) {
            logger.warn("Failed to cleanup temporary directory for fileId: {}", fileId, e);
        }
    }

    /**
     * Reactive version of cleanup
     */
    public Mono<Void> cleanupTempDirectoryReactive(String fileId) {
        return Mono.fromRunnable(() -> cleanupTempDirectory(fileId))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    /**
     * Gets the temporary directory path for a file ID
     */
    public Path getTempDirectory(String fileId) {
        return uploadDir.resolve(tempDirPrefix + fileId);
    }

    /**
     * Gets the base upload directory
     */
    public Path getUploadDirectory() {
        return uploadDir;
    }

    /**
     * Creates temporary directory if it doesn't exist
     */
    private void createTempDirectoryIfNotExists(Path tempUploadDir, String fileId) throws IOException {
        if (!Files.exists(tempUploadDir)) {
            try {
                Files.createDirectories(tempUploadDir);
                logger.debug("Created temporary directory for fileId: {} at path: {}", fileId, tempUploadDir);
            } catch (IOException e) {
                logger.error("Failed to create temporary directory for fileId: {} at path: {}", fileId, tempUploadDir, e);
                throw new FileStorageException("Failed to create temporary directory", fileId, "CREATE_TEMP_DIR", e);
            }
        }
    }

    /**
     * Validates that all chunks exist for a file
     */
    public boolean allChunksExist(String fileId, int totalChunks) {
        for (int i = 0; i < totalChunks; i++) {
            if (!chunkExists(fileId, i)) {
                logger.debug("Missing chunk {} for fileId: {}", i, fileId);
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the size of a specific chunk
     */
    public long getChunkSize(String fileId, int chunkNumber) throws IOException {
        Path chunkPath = getChunkPath(fileId, chunkNumber);
        if (!Files.exists(chunkPath)) {
            throw new FileStorageException("Chunk file does not exist", fileId, "GET_CHUNK_SIZE");
        }
        return Files.size(chunkPath);
    }

    /**
     * Gets the total size of all chunks for a file
     */
    public long getTotalChunksSize(String fileId, int totalChunks) throws IOException {
        long totalSize = 0;
        for (int i = 0; i < totalChunks; i++) {
            totalSize += getChunkSize(fileId, i);
        }
        return totalSize;
    }
}
