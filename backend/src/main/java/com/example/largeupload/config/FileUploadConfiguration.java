package com.example.largeupload.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for file upload functionality
 */
@Configuration
@EnableConfigurationProperties(FileUploadProperties.class)
public class FileUploadConfiguration {
    // This class enables the FileUploadProperties configuration
}
