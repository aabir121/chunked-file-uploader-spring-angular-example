package com.example.largeupload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for file upload functionality
 */
@Data
@ConfigurationProperties(prefix = "file-upload")
public class FileUploadProperties {

    private Storage storage = new Storage();
    private Chunk chunk = new Chunk();
    private File file = new File();
    private Cleanup cleanup = new Cleanup();
    private Validation validation = new Validation();
    private Performance performance = new Performance();
    private Cors cors = new Cors();



    @Data
    public static class Storage {
        private String baseDirectory = "uploads";
        private String tempDirectoryPrefix = "temp_";
    }

    @Data
    public static class Chunk {
        private long defaultSize = 5L * 1024 * 1024; // 5MB
        private long maxSize = 100L * 1024 * 1024; // 100MB
        private int maxCount = 10000;
    }

    @Data
    public static class File {
        private long maxSize = 50L * 1024 * 1024 * 1024; // 50GB
    }

    @Data
    public static class Cleanup {
        private boolean autoCleanupEnabled = true;
        private int cleanupDelayHours = 24;
    }

    @Data
    public static class Validation {
        private List<String> allowedExtensions = List.of();
        private List<String> blockedExtensions = List.of("exe", "bat", "cmd", "scr", "com", "pif");
    }

    @Data
    public static class Performance {
        private int maxConcurrentUploads = 10;
        private int ioThreadPoolSize = 4;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:4200", "http://localhost:4201");
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        private List<String> allowedHeaders = List.of("*");
        private boolean allowCredentials = true;
        private long maxAge = 3600;
    }
}
