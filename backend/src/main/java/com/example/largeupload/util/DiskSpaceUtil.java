package com.example.largeupload.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for disk space operations and checks
 */
public class DiskSpaceUtil {

    private static final Logger logger = LoggerFactory.getLogger(DiskSpaceUtil.class);

    // Minimum free space threshold (100MB)
    private static final long MIN_FREE_SPACE_BYTES = 100L * 1024 * 1024;

    // Safety buffer for disk space calculations (50MB)
    private static final long SAFETY_BUFFER_BYTES = 50L * 1024 * 1024;

    /**
     * Private constructor to prevent instantiation of utility class
     */
    private DiskSpaceUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Checks if there's enough disk space for the given operation
     *
     * @param path The path to check disk space for
     * @param requiredBytes The number of bytes required
     * @return true if there's enough space, false otherwise
     */
    public static boolean hasEnoughSpace(Path path, long requiredBytes) {
        return hasEnoughSpace(path, requiredBytes, MIN_FREE_SPACE_BYTES, SAFETY_BUFFER_BYTES);
    }

    /**
     * Checks if there's enough disk space for the given operation with custom thresholds
     *
     * @param path The path to check disk space for
     * @param requiredBytes The number of bytes required
     * @param minFreeSpaceBytes Minimum free space to maintain
     * @param safetyBufferBytes Safety buffer to add to required bytes
     * @return true if there's enough space, false otherwise
     */
    public static boolean hasEnoughSpace(Path path, long requiredBytes, long minFreeSpaceBytes, long safetyBufferBytes) {
        try {
            FileStore fileStore = Files.getFileStore(path);
            long usableSpace = fileStore.getUsableSpace();
            long totalRequiredSpace = requiredBytes + safetyBufferBytes;

            boolean hasSpace = usableSpace >= totalRequiredSpace && usableSpace >= minFreeSpaceBytes;

            logger.debug("Disk space check for path: {}, required: {} bytes, available: {} bytes, has space: {}",
                        path, requiredBytes, usableSpace, hasSpace);

            return hasSpace;
        } catch (IOException e) {
            logger.warn("Failed to check disk space for path: {}", path, e);
            return false; // Assume no space if we can't check
        }
    }

    /**
     * Gets the available disk space for the given path
     * 
     * @param path The path to check
     * @return Available space in bytes, or -1 if unable to determine
     */
    public static long getAvailableSpace(Path path) {
        try {
            FileStore fileStore = Files.getFileStore(path);
            return fileStore.getUsableSpace();
        } catch (IOException e) {
            logger.warn("Failed to get available disk space for path: {}", path, e);
            return -1;
        }
    }

    /**
     * Gets the total disk space for the given path
     * 
     * @param path The path to check
     * @return Total space in bytes, or -1 if unable to determine
     */
    public static long getTotalSpace(Path path) {
        try {
            FileStore fileStore = Files.getFileStore(path);
            return fileStore.getTotalSpace();
        } catch (IOException e) {
            logger.warn("Failed to get total disk space for path: {}", path, e);
            return -1;
        }
    }

    /**
     * Checks if the IOException is related to insufficient disk space
     * 
     * @param ioException The IOException to check
     * @return true if the exception indicates insufficient disk space
     */
    public static boolean isInsufficientSpaceException(IOException ioException) {
        if (ioException == null) {
            return false;
        }
        
        String message = ioException.getMessage();
        if (message == null) {
            return false;
        }
        
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("not enough space") ||
               lowerMessage.contains("insufficient space") ||
               lowerMessage.contains("no space left") ||
               lowerMessage.contains("disk full") ||
               lowerMessage.contains("out of space");
    }

    /**
     * Formats bytes into a human-readable string
     * 
     * @param bytes The number of bytes
     * @return Formatted string (e.g., "1.5 GB", "256 MB")
     */
    public static String formatBytes(long bytes) {
        if (bytes < 0) {
            return "Unknown";
        }
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }

    /**
     * Gets disk space information as a formatted string
     * 
     * @param path The path to check
     * @return Formatted disk space information
     */
    public static String getDiskSpaceInfo(Path path) {
        try {
            FileStore fileStore = Files.getFileStore(path);
            long total = fileStore.getTotalSpace();
            long available = fileStore.getUsableSpace();
            long used = total - available;
            
            double usedPercentage = total > 0 ? (used * 100.0) / total : 0;
            
            return String.format("Disk space for %s: %s used (%.1f%%), %s available, %s total", 
                               path, formatBytes(used), usedPercentage, formatBytes(available), formatBytes(total));
        } catch (IOException e) {
            return String.format("Unable to determine disk space for %s: %s", path, e.getMessage());
        }
    }

    /**
     * Validates that there's enough space before performing an operation
     *
     * @param path The path where the operation will occur
     * @param requiredBytes The bytes required for the operation
     * @param operationName The name of the operation (for logging)
     * @throws IOException if there's insufficient space
     */
    public static void validateDiskSpace(Path path, long requiredBytes, String operationName) throws IOException {
        validateDiskSpace(path, requiredBytes, operationName, MIN_FREE_SPACE_BYTES, SAFETY_BUFFER_BYTES);
    }

    /**
     * Validates that there's enough space before performing an operation with custom thresholds
     *
     * @param path The path where the operation will occur
     * @param requiredBytes The bytes required for the operation
     * @param operationName The name of the operation (for logging)
     * @param minFreeSpaceBytes Minimum free space to maintain
     * @param safetyBufferBytes Safety buffer to add to required bytes
     * @throws IOException if there's insufficient space
     */
    public static void validateDiskSpace(Path path, long requiredBytes, String operationName,
                                       long minFreeSpaceBytes, long safetyBufferBytes) throws IOException {
        if (!hasEnoughSpace(path, requiredBytes, minFreeSpaceBytes, safetyBufferBytes)) {
            long availableSpace = getAvailableSpace(path);
            String errorMessage = String.format(
                "Insufficient disk space for %s. Required: %s, Available: %s, Path: %s",
                operationName,
                formatBytes(requiredBytes),
                formatBytes(availableSpace),
                path
            );

            logger.error(errorMessage);
            if (logger.isInfoEnabled()) {
                logger.info(getDiskSpaceInfo(path));
            }

            throw new IOException("There is not enough space on the disk. " + errorMessage);
        }
    }
}
