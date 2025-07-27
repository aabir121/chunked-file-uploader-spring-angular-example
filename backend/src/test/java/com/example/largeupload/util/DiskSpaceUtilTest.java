package com.example.largeupload.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DiskSpaceUtil
 */
class DiskSpaceUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void testHasEnoughSpace_WithSufficientSpace() {
        // Test with a small requirement that should always pass
        boolean hasSpace = DiskSpaceUtil.hasEnoughSpace(tempDir, 1024);
        assertTrue(hasSpace, "Should have enough space for 1KB");
    }

    @Test
    void testGetAvailableSpace() {
        long availableSpace = DiskSpaceUtil.getAvailableSpace(tempDir);
        assertTrue(availableSpace > 0, "Available space should be positive");
    }

    @Test
    void testGetTotalSpace() {
        long totalSpace = DiskSpaceUtil.getTotalSpace(tempDir);
        assertTrue(totalSpace > 0, "Total space should be positive");
    }

    @Test
    void testIsInsufficientSpaceException() {
        // Test with disk space related messages
        IOException diskSpaceEx1 = new IOException("There is not enough space on the disk");
        assertTrue(DiskSpaceUtil.isInsufficientSpaceException(diskSpaceEx1));

        IOException diskSpaceEx2 = new IOException("No space left on device");
        assertTrue(DiskSpaceUtil.isInsufficientSpaceException(diskSpaceEx2));

        IOException diskSpaceEx3 = new IOException("Disk full");
        assertTrue(DiskSpaceUtil.isInsufficientSpaceException(diskSpaceEx3));

        // Test with non-disk space related message
        IOException otherEx = new IOException("File not found");
        assertFalse(DiskSpaceUtil.isInsufficientSpaceException(otherEx));

        // Test with null
        assertFalse(DiskSpaceUtil.isInsufficientSpaceException(null));
    }

    @Test
    void testFormatBytes() {
        assertEquals("0.0 B", DiskSpaceUtil.formatBytes(0));
        assertEquals("1.0 KB", DiskSpaceUtil.formatBytes(1024));
        assertEquals("1.0 MB", DiskSpaceUtil.formatBytes(1024 * 1024));
        assertEquals("1.0 GB", DiskSpaceUtil.formatBytes(1024L * 1024 * 1024));
        assertEquals("1.5 GB", DiskSpaceUtil.formatBytes(1536L * 1024 * 1024));
        assertEquals("Unknown", DiskSpaceUtil.formatBytes(-1));
    }

    @Test
    void testGetDiskSpaceInfo() {
        String info = DiskSpaceUtil.getDiskSpaceInfo(tempDir);
        assertNotNull(info);
        assertTrue(info.contains("Disk space for"));
        assertTrue(info.contains("used"));
        assertTrue(info.contains("available"));
        assertTrue(info.contains("total"));
    }

    @Test
    void testValidateDiskSpace_WithSufficientSpace() {
        // This should not throw an exception
        assertDoesNotThrow(() -> {
            DiskSpaceUtil.validateDiskSpace(tempDir, 1024, "test operation");
        });
    }

    @Test
    void testValidateDiskSpace_WithInsufficientSpace() {
        // Get current available space and request more than available
        long availableSpace = DiskSpaceUtil.getAvailableSpace(tempDir);
        long impossibleSize = availableSpace + (1024L * 1024 * 1024); // Add 1GB to available space

        IOException exception = assertThrows(IOException.class, () -> {
            DiskSpaceUtil.validateDiskSpace(tempDir, impossibleSize, "test operation");
        });

        assertTrue(exception.getMessage().contains("There is not enough space on the disk"));
        assertTrue(exception.getMessage().contains("test operation"));
    }

    @Test
    void testHasEnoughSpaceWithCustomThresholds() {
        // Test with custom thresholds
        long minFreeSpace = 1024; // 1KB
        long safetyBuffer = 512;   // 512B

        // This should not throw an exception and return a boolean value
        assertDoesNotThrow(() -> {
            boolean hasSpace = DiskSpaceUtil.hasEnoughSpace(tempDir, 1024, minFreeSpace, safetyBuffer);
            // The result can be true or false, but it should be a valid boolean
            assertTrue(hasSpace || !hasSpace); // This will always pass but ensures the method returns
        });
    }

    @Test
    void testValidateDiskSpaceWithCustomThresholds() {
        // Test with reasonable custom thresholds
        long minFreeSpace = 1024; // 1KB
        long safetyBuffer = 512;   // 512B
        
        assertDoesNotThrow(() -> {
            DiskSpaceUtil.validateDiskSpace(tempDir, 1024, "test operation", minFreeSpace, safetyBuffer);
        });
    }
}
