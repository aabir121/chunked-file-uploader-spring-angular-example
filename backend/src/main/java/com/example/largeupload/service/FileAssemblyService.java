package com.example.largeupload.service;

import com.example.largeupload.exception.FileStorageException;
import com.example.largeupload.util.DiskSpaceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Service responsible for assembling file chunks into final files
 */
@Service
public class FileAssemblyService {

    private static final Logger logger = LoggerFactory.getLogger(FileAssemblyService.class);
    
    private final ChunkStorageService chunkStorageService;

    public FileAssemblyService(ChunkStorageService chunkStorageService) {
        this.chunkStorageService = chunkStorageService;
    }

    /**
     * Assembles all chunks into a final file
     */
    public void assembleFile(String fileId, int totalChunks, String fileName) throws IOException {
        logger.info("Starting file assembly for fileId: {}, totalChunks: {}, fileName: {}", 
                   fileId, totalChunks, fileName);

        // Validate all chunks exist
        if (!chunkStorageService.allChunksExist(fileId, totalChunks)) {
            throw new FileStorageException("Not all chunks are available for assembly", fileId, "ASSEMBLE");
        }

        // Get chunk files
        Path[] chunkFiles = chunkStorageService.getChunkFiles(fileId, totalChunks);
        
        // Determine final file path
        Path finalFilePath = determineFinalFilePath(fileName, fileId);
        
        // Assemble chunks using efficient file channel operations
        assembleChunksToFile(chunkFiles, finalFilePath, fileId);
        
        logger.info("Successfully assembled file for fileId: {} at path: {}", fileId, finalFilePath);
    }

    /**
     * Reactive version of assembleFile
     */
    public Mono<Void> assembleFileReactive(String fileId, int totalChunks, String fileName) {
        return Mono.fromCallable(() -> {
            try {
                assembleFile(fileId, totalChunks, fileName);
                return null;
            } catch (IOException e) {
                logger.error("Reactive file assembly failed for fileId: {}", fileId, e);
                throw new FileStorageException("Failed to assemble file reactively", fileId, "ASSEMBLE_REACTIVE", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    /**
     * Assembles chunks to a specific target file path
     */
    public void assembleChunksToFile(Path[] chunkFiles, Path targetFile, String fileId) throws IOException {
        logger.debug("Assembling {} chunks to file: {}", chunkFiles.length, targetFile);

        // Calculate total size needed for assembly
        long totalSizeNeeded = 0;
        for (Path chunkFile : chunkFiles) {
            if (Files.exists(chunkFile)) {
                totalSizeNeeded += Files.size(chunkFile);
            }
        }

        // Check disk space before starting assembly
        try {
            DiskSpaceUtil.validateDiskSpace(targetFile.getParent(), totalSizeNeeded,
                "file assembly for fileId: " + fileId);
        } catch (IOException diskSpaceEx) {
            logger.error("Insufficient disk space for file assembly of fileId: {}, required: {} bytes",
                        fileId, totalSizeNeeded, diskSpaceEx);
            throw new FileStorageException("Insufficient disk space for file assembly", fileId, "ASSEMBLE", "INSUFFICIENT_DISK_SPACE", diskSpaceEx);
        }

        try (FileChannel outputChannel = FileChannel.open(targetFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            
            long totalBytesWritten = 0;
            
            for (int i = 0; i < chunkFiles.length; i++) {
                Path chunkFile = chunkFiles[i];
                
                if (!Files.exists(chunkFile)) {
                    throw new FileStorageException("Chunk file missing during assembly: " + i, fileId, "ASSEMBLE");
                }
                
                try (FileChannel inputChannel = FileChannel.open(chunkFile, StandardOpenOption.READ)) {
                    long chunkSize = inputChannel.size();
                    long bytesTransferred = inputChannel.transferTo(0, chunkSize, outputChannel);
                    
                    if (bytesTransferred != chunkSize) {
                        throw new FileStorageException(
                            "Incomplete chunk transfer for chunk " + i + ": expected " + chunkSize + " bytes, transferred " + bytesTransferred,
                            fileId, "ASSEMBLE"
                        );
                    }
                    
                    totalBytesWritten += bytesTransferred;
                    logger.debug("Transferred chunk {} ({} bytes) for fileId: {}", i, bytesTransferred, fileId);
                }
            }
            
            logger.info("File assembly completed for fileId: {}. Total bytes written: {}", fileId, totalBytesWritten);
            
        } catch (IOException e) {
            logger.error("Failed to assemble chunks for fileId: {} to file: {}", fileId, targetFile, e);

            // Clean up partially written file
            try {
                if (Files.exists(targetFile)) {
                    Files.delete(targetFile);
                    logger.debug("Cleaned up partially written file: {}", targetFile);
                }
            } catch (IOException cleanupEx) {
                logger.warn("Failed to cleanup partially written file: {}", targetFile, cleanupEx);
            }

            // Check if this is a disk space related error
            if (DiskSpaceUtil.isInsufficientSpaceException(e)) {
                logger.error("Disk space error detected during file assembly for fileId: {}", fileId);
                logger.info(DiskSpaceUtil.getDiskSpaceInfo(targetFile.getParent()));
                throw new FileStorageException("Insufficient disk space during file assembly", fileId, "ASSEMBLE", "INSUFFICIENT_DISK_SPACE", e);
            }

            throw new FileStorageException("Failed to assemble file", fileId, "ASSEMBLE", e);
        }
    }

    /**
     * Validates file integrity after assembly
     */
    public boolean validateAssembledFile(String fileId, int totalChunks, Path assembledFile) {
        try {
            if (!Files.exists(assembledFile)) {
                logger.warn("Assembled file does not exist for fileId: {}", fileId);
                return false;
            }

            long assembledFileSize = Files.size(assembledFile);
            long expectedSize = chunkStorageService.getTotalChunksSize(fileId, totalChunks);
            
            if (assembledFileSize != expectedSize) {
                logger.warn("File size mismatch for fileId: {}. Expected: {}, Actual: {}", 
                           fileId, expectedSize, assembledFileSize);
                return false;
            }

            logger.debug("File validation successful for fileId: {}. Size: {} bytes", fileId, assembledFileSize);
            return true;
            
        } catch (IOException e) {
            logger.error("Failed to validate assembled file for fileId: {}", fileId, e);
            return false;
        }
    }

    /**
     * Determines the final file path, handling naming conflicts
     */
    private Path determineFinalFilePath(String fileName, String fileId) {
        Path uploadDir = chunkStorageService.getUploadDirectory();
        
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = fileId + ".bin"; // Default extension for unknown files
        }
        
        Path finalPath = uploadDir.resolve(fileName);
        
        // Handle naming conflicts by appending a counter
        int counter = 1;
        while (Files.exists(finalPath)) {
            String nameWithoutExtension = getNameWithoutExtension(fileName);
            String extension = getFileExtension(fileName);
            String newFileName = nameWithoutExtension + "_" + counter + (extension.isEmpty() ? "" : "." + extension);
            finalPath = uploadDir.resolve(newFileName);
            counter++;
        }
        
        return finalPath;
    }

    /**
     * Gets file name without extension
     */
    private String getNameWithoutExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }

    /**
     * Gets file extension
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1);
        }
        return "";
    }

    /**
     * Estimates the final file size based on chunks
     */
    public long estimateFinalFileSize(String fileId, int totalChunks) throws IOException {
        return chunkStorageService.getTotalChunksSize(fileId, totalChunks);
    }

    /**
     * Checks if assembly is possible (all chunks available)
     */
    public boolean canAssemble(String fileId, int totalChunks) {
        return chunkStorageService.allChunksExist(fileId, totalChunks);
    }

    /**
     * Gets information about missing chunks
     */
    public int[] getMissingChunks(String fileId, int totalChunks) {
        return java.util.stream.IntStream.range(0, totalChunks)
                .filter(i -> !chunkStorageService.chunkExists(fileId, i))
                .toArray();
    }
}
