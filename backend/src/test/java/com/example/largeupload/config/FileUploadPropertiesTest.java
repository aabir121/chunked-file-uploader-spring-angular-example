package com.example.largeupload.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that FileUploadProperties configuration is properly loaded and respected
 */
@SpringBootTest
@TestPropertySource(properties = {
    "file-upload.storage.base-directory=test-uploads",
    "file-upload.chunk.default-size=1048576",
    "file-upload.chunk.max-size=52428800", 
    "file-upload.file.max-size=1073741824",
    "file-upload.cleanup.auto-cleanup-enabled=false",
    "file-upload.cleanup.cleanup-delay-hours=48",
    "file-upload.validation.allowed-extensions=pdf,txt,doc",
    "file-upload.validation.blocked-extensions=exe,bat",
    "file-upload.performance.max-concurrent-uploads=5",
    "file-upload.performance.io-thread-pool-size=2",
    "file-upload.cors.allowed-origins=https://test.com,https://app.test.com",
    "file-upload.cors.allowed-methods=GET,POST",
    "file-upload.cors.allow-credentials=false",
    "file-upload.cors.max-age=7200"
})
class FileUploadPropertiesTest {

    @Autowired
    private FileUploadProperties properties;

    @Test
    void testStorageProperties() {
        assertEquals("test-uploads", properties.getStorage().getBaseDirectory());
        assertEquals("temp_", properties.getStorage().getTempDirectoryPrefix());
    }

    @Test
    void testChunkProperties() {
        assertEquals(1048576L, properties.getChunk().getDefaultSize()); // 1MB
        assertEquals(52428800L, properties.getChunk().getMaxSize()); // 50MB
        assertEquals(10000, properties.getChunk().getMaxCount()); // Default value
    }

    @Test
    void testFileProperties() {
        assertEquals(1073741824L, properties.getFile().getMaxSize()); // 1GB
    }

    @Test
    void testCleanupProperties() {
        assertFalse(properties.getCleanup().isAutoCleanupEnabled());
        assertEquals(48, properties.getCleanup().getCleanupDelayHours());
    }

    @Test
    void testValidationProperties() {
        assertEquals(3, properties.getValidation().getAllowedExtensions().size());
        assertTrue(properties.getValidation().getAllowedExtensions().contains("pdf"));
        assertTrue(properties.getValidation().getAllowedExtensions().contains("txt"));
        assertTrue(properties.getValidation().getAllowedExtensions().contains("doc"));
        
        assertEquals(2, properties.getValidation().getBlockedExtensions().size());
        assertTrue(properties.getValidation().getBlockedExtensions().contains("exe"));
        assertTrue(properties.getValidation().getBlockedExtensions().contains("bat"));
    }

    @Test
    void testPerformanceProperties() {
        assertEquals(5, properties.getPerformance().getMaxConcurrentUploads());
        assertEquals(2, properties.getPerformance().getIoThreadPoolSize());
    }

    @Test
    void testCorsProperties() {
        assertEquals(2, properties.getCors().getAllowedOrigins().size());
        assertTrue(properties.getCors().getAllowedOrigins().contains("https://test.com"));
        assertTrue(properties.getCors().getAllowedOrigins().contains("https://app.test.com"));
        
        assertEquals(2, properties.getCors().getAllowedMethods().size());
        assertTrue(properties.getCors().getAllowedMethods().contains("GET"));
        assertTrue(properties.getCors().getAllowedMethods().contains("POST"));
        
        assertFalse(properties.getCors().isAllowCredentials());
        assertEquals(7200L, properties.getCors().getMaxAge());
    }

    @Test
    void testDefaultValues() {
        // Test that default values are preserved when not overridden
        FileUploadProperties defaultProps = new FileUploadProperties();
        
        // These should match the defaults in the class
        assertEquals("uploads", defaultProps.getStorage().getBaseDirectory());
        assertEquals(5L * 1024 * 1024, defaultProps.getChunk().getDefaultSize()); // 5MB
        assertEquals(100L * 1024 * 1024, defaultProps.getChunk().getMaxSize()); // 100MB
        assertEquals(50L * 1024 * 1024 * 1024, defaultProps.getFile().getMaxSize()); // 50GB
        assertTrue(defaultProps.getCleanup().isAutoCleanupEnabled());
        assertEquals(24, defaultProps.getCleanup().getCleanupDelayHours());
        assertEquals(10, defaultProps.getPerformance().getMaxConcurrentUploads());
        assertEquals(4, defaultProps.getPerformance().getIoThreadPoolSize());
        assertTrue(defaultProps.getCors().isAllowCredentials());
        assertEquals(3600L, defaultProps.getCors().getMaxAge());
    }
}
