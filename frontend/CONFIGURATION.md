# Configuration Guide

This document provides comprehensive information about configuring the Large File Upload Frontend application.

## 📋 Table of Contents

- [Environment Configuration](#environment-configuration)
- [API Configuration](#api-configuration)
- [Upload Configuration](#upload-configuration)
- [UI Configuration](#ui-configuration)
- [Logging Configuration](#logging-configuration)
- [Development vs Production](#development-vs-production)
- [Configuration Validation](#configuration-validation)
- [Advanced Configuration](#advanced-configuration)

## 🌍 Environment Configuration

The application uses Angular's environment system with support for runtime environment variables in production.

### Environment Files

- `src/environments/environment.ts` - Development configuration
- `src/environments/environment.prod.ts` - Production configuration

### Environment Structure

```typescript
export const environment = {
  production: boolean,
  api: ApiConfig,
  upload: UploadConfig,
  ui: UiConfig,
  logging: LoggingConfig
};
```

## 🔌 API Configuration

Configure backend API endpoints and connection settings.

### Configuration Options

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `baseUrl` | string | `'http://localhost:8080'` | Backend API base URL |
| `uploadEndpoint` | string | `'/upload'` | Upload endpoint path |
| `binaryUploadEndpoint` | string | `'/upload/binary'` | Binary upload endpoint path |
| `statusEndpoint` | string | `'/upload/status'` | Status check endpoint path |

### Environment Variables (Production)

```bash
# API Configuration
API_BASE_URL=https://api.yourapp.com
UPLOAD_ENDPOINT=/api/v1/upload
BINARY_UPLOAD_ENDPOINT=/api/v1/upload/binary
STATUS_ENDPOINT=/api/v1/upload/status
```

### Example Configuration

```typescript
// Development
api: {
  baseUrl: 'http://localhost:8080',
  uploadEndpoint: '/upload',
  binaryUploadEndpoint: '/upload/binary',
  statusEndpoint: '/upload/status'
}

// Production (using environment variables)
api: {
  baseUrl: process.env['API_BASE_URL'] || 'https://api.yourapp.com',
  uploadEndpoint: process.env['UPLOAD_ENDPOINT'] || '/upload',
  binaryUploadEndpoint: process.env['BINARY_UPLOAD_ENDPOINT'] || '/upload/binary',
  statusEndpoint: process.env['STATUS_ENDPOINT'] || '/upload/status'
}
```

## 📤 Upload Configuration

Configure upload behavior, performance, and reliability settings.

### Configuration Options

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `chunkSize` | number | `5242880` (5MB) | Size of each upload chunk in bytes |
| `maxConcurrentUploads` | number | `3` | Maximum number of concurrent uploads |
| `retryAttempts` | number | `3` | Number of retry attempts for failed chunks |
| `retryDelay` | number | `1000` | Base delay between retries (ms) |
| `timeoutMs` | number | `30000` | Request timeout in milliseconds |

### Environment Variables (Production)

```bash
# Upload Configuration
CHUNK_SIZE=5242880                    # 5MB chunks
MAX_CONCURRENT_UPLOADS=3              # 3 concurrent uploads
RETRY_ATTEMPTS=3                      # 3 retry attempts
RETRY_DELAY=1000                      # 1 second base delay
TIMEOUT_MS=30000                      # 30 second timeout
```

### Chunk Size Guidelines

| File Size Range | Recommended Chunk Size | Reasoning |
|------------------|------------------------|-----------|
| < 10MB | 1MB | Faster initial response |
| 10MB - 100MB | 5MB | Balanced performance |
| 100MB - 1GB | 10MB | Reduced overhead |
| > 1GB | 20MB | Maximum efficiency |

### Performance Tuning

```typescript
// High-performance configuration
upload: {
  chunkSize: 10 * 1024 * 1024,        // 10MB chunks
  maxConcurrentUploads: 5,             // More concurrent uploads
  retryAttempts: 5,                    // More retries
  retryDelay: 500,                     // Faster retries
  timeoutMs: 60000                     // Longer timeout
}

// Conservative configuration (slow networks)
upload: {
  chunkSize: 1 * 1024 * 1024,         // 1MB chunks
  maxConcurrentUploads: 1,             // Single upload
  retryAttempts: 10,                   // Many retries
  retryDelay: 2000,                    // Longer delays
  timeoutMs: 120000                    // Very long timeout
}
```

## 🎨 UI Configuration

Configure user interface behavior and limits.

### Configuration Options

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `progressUpdateInterval` | number | `100` | Progress update frequency (ms) |
| `autoCloseModalDelay` | number | `2000` | Modal auto-close delay (ms) |
| `maxFileSize` | number | `10737418240` (10GB) | Maximum file size in bytes |
| `allowedFileTypes` | string[] | `['*']` | Allowed MIME types |

### Environment Variables (Production)

```bash
# UI Configuration
PROGRESS_UPDATE_INTERVAL=100          # 100ms progress updates
AUTO_CLOSE_MODAL_DELAY=2000          # 2 second modal delay
MAX_FILE_SIZE=10737418240            # 10GB max file size
ALLOWED_FILE_TYPES=image/*,video/*,application/pdf  # Specific types
```

### File Type Configuration

```typescript
// Allow all file types
ui: {
  allowedFileTypes: ['*']
}

// Allow specific types
ui: {
  allowedFileTypes: [
    'image/*',
    'video/*',
    'application/pdf',
    'application/msword',
    'text/plain'
  ]
}

// Allow specific extensions (converted to MIME types)
ui: {
  allowedFileTypes: [
    'image/jpeg',
    'image/png',
    'video/mp4',
    'application/pdf'
  ]
}
```

### File Size Limits

```typescript
// Different size limits for different environments
const fileSizeLimits = {
  development: 100 * 1024 * 1024,      // 100MB
  staging: 1024 * 1024 * 1024,         // 1GB
  production: 10 * 1024 * 1024 * 1024  // 10GB
};
```

## 📊 Logging Configuration

Configure application logging and monitoring.

### Configuration Options

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `level` | string | `'info'` | Logging level (error, warn, info, debug) |
| `enableConsoleLogging` | boolean | `true` | Enable console output |
| `enableRemoteLogging` | boolean | `false` | Enable remote logging |

### Environment Variables (Production)

```bash
# Logging Configuration
LOG_LEVEL=warn                        # Production log level
ENABLE_CONSOLE_LOGGING=false         # Disable console in production
ENABLE_REMOTE_LOGGING=true           # Enable remote logging
```

### Log Levels

| Level | Description | Use Case |
|-------|-------------|----------|
| `error` | Error messages only | Production (minimal logging) |
| `warn` | Warnings and errors | Production (standard) |
| `info` | Informational messages | Staging/Development |
| `debug` | Detailed debug information | Development only |

### Logging Examples

```typescript
// Production logging
logging: {
  level: 'warn',
  enableConsoleLogging: false,
  enableRemoteLogging: true
}

// Development logging
logging: {
  level: 'debug',
  enableConsoleLogging: true,
  enableRemoteLogging: false
}

// Staging logging
logging: {
  level: 'info',
  enableConsoleLogging: true,
  enableRemoteLogging: true
}
```

## 🔄 Development vs Production

### Development Configuration

```typescript
// src/environments/environment.ts
export const environment = {
  production: false,
  api: {
    baseUrl: 'http://localhost:8080',
    uploadEndpoint: '/upload',
    binaryUploadEndpoint: '/upload/binary',
    statusEndpoint: '/upload/status'
  },
  upload: {
    chunkSize: 5 * 1024 * 1024,
    maxConcurrentUploads: 3,
    retryAttempts: 3,
    retryDelay: 1000,
    timeoutMs: 30000
  },
  ui: {
    progressUpdateInterval: 100,
    autoCloseModalDelay: 2000,
    maxFileSize: 10 * 1024 * 1024 * 1024,
    allowedFileTypes: ['*']
  },
  logging: {
    level: 'debug',
    enableConsoleLogging: true,
    enableRemoteLogging: false
  }
};
```

### Production Configuration

```typescript
// src/environments/environment.prod.ts
export const environment = {
  production: true,
  api: {
    baseUrl: process.env['API_BASE_URL'] || 'https://api.yourapp.com',
    uploadEndpoint: process.env['UPLOAD_ENDPOINT'] || '/upload',
    binaryUploadEndpoint: process.env['BINARY_UPLOAD_ENDPOINT'] || '/upload/binary',
    statusEndpoint: process.env['STATUS_ENDPOINT'] || '/upload/status'
  },
  upload: {
    chunkSize: parseInt(process.env['CHUNK_SIZE'] || '5242880'),
    maxConcurrentUploads: parseInt(process.env['MAX_CONCURRENT_UPLOADS'] || '3'),
    retryAttempts: parseInt(process.env['RETRY_ATTEMPTS'] || '3'),
    retryDelay: parseInt(process.env['RETRY_DELAY'] || '1000'),
    timeoutMs: parseInt(process.env['TIMEOUT_MS'] || '30000')
  },
  ui: {
    progressUpdateInterval: parseInt(process.env['PROGRESS_UPDATE_INTERVAL'] || '100'),
    autoCloseModalDelay: parseInt(process.env['AUTO_CLOSE_MODAL_DELAY'] || '2000'),
    maxFileSize: parseInt(process.env['MAX_FILE_SIZE'] || '10737418240'),
    allowedFileTypes: process.env['ALLOWED_FILE_TYPES']?.split(',') || ['*']
  },
  logging: {
    level: process.env['LOG_LEVEL'] || 'info',
    enableConsoleLogging: process.env['ENABLE_CONSOLE_LOGGING'] !== 'false',
    enableRemoteLogging: process.env['ENABLE_REMOTE_LOGGING'] === 'true'
  }
};
```

## ✅ Configuration Validation

The application includes built-in configuration validation to ensure settings are valid and safe.

### Validation Rules

#### API Configuration
- `baseUrl` must be a valid URL
- Endpoint paths must start with '/'
- URLs must use HTTP or HTTPS protocols

#### Upload Configuration
- `chunkSize` must be between 1MB and 100MB
- `maxConcurrentUploads` must be between 1 and 10
- `retryAttempts` must be between 0 and 10
- `retryDelay` must be between 100ms and 60000ms
- `timeoutMs` must be between 5000ms and 300000ms

#### UI Configuration
- `progressUpdateInterval` must be between 50ms and 1000ms
- `autoCloseModalDelay` must be between 0ms and 10000ms
- `maxFileSize` must be between 1MB and 100GB
- `allowedFileTypes` must be valid MIME types or '*'

#### Logging Configuration
- `level` must be one of: 'error', 'warn', 'info', 'debug'
- Boolean flags must be actual booleans

### Configuration Examples

#### Small Files Configuration
```typescript
// Optimized for many small files
{
  upload: {
    chunkSize: 1 * 1024 * 1024,        // 1MB chunks
    maxConcurrentUploads: 5,           // More concurrent uploads
    retryAttempts: 3,
    retryDelay: 500,
    timeoutMs: 15000                   // Shorter timeout
  },
  ui: {
    maxFileSize: 100 * 1024 * 1024,    // 100MB max
    progressUpdateInterval: 50         // Faster updates
  }
}
```

#### Large Files Configuration
```typescript
// Optimized for large files
{
  upload: {
    chunkSize: 20 * 1024 * 1024,       // 20MB chunks
    maxConcurrentUploads: 2,           // Fewer concurrent uploads
    retryAttempts: 5,                  // More retries
    retryDelay: 2000,                  // Longer delays
    timeoutMs: 120000                  // Longer timeout
  },
  ui: {
    maxFileSize: 100 * 1024 * 1024 * 1024, // 100GB max
    progressUpdateInterval: 200        // Less frequent updates
  }
}
```

#### High-Security Configuration
```typescript
// Security-focused configuration
{
  ui: {
    allowedFileTypes: [
      'image/jpeg',
      'image/png',
      'application/pdf',
      'text/plain'
    ],
    maxFileSize: 10 * 1024 * 1024      // 10MB max
  },
  logging: {
    level: 'warn',                     // Minimal logging
    enableConsoleLogging: false,
    enableRemoteLogging: true
  }
}
```

## 🚀 Deployment Configuration

### Environment Variables Reference

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `API_BASE_URL` | string | `http://localhost:8080` | Backend API URL |
| `UPLOAD_ENDPOINT` | string | `/upload` | Upload endpoint path |
| `BINARY_UPLOAD_ENDPOINT` | string | `/upload/binary` | Binary upload path |
| `STATUS_ENDPOINT` | string | `/upload/status` | Status endpoint path |
| `CHUNK_SIZE` | number | `5242880` | Chunk size in bytes |
| `MAX_CONCURRENT_UPLOADS` | number | `3` | Max concurrent uploads |
| `RETRY_ATTEMPTS` | number | `3` | Retry attempts |
| `RETRY_DELAY` | number | `1000` | Retry delay (ms) |
| `TIMEOUT_MS` | number | `30000` | Request timeout (ms) |
| `PROGRESS_UPDATE_INTERVAL` | number | `100` | Progress update interval (ms) |
| `AUTO_CLOSE_MODAL_DELAY` | number | `2000` | Modal close delay (ms) |
| `MAX_FILE_SIZE` | number | `10737418240` | Max file size (bytes) |
| `ALLOWED_FILE_TYPES` | string | `*` | Allowed MIME types (comma-separated) |
| `LOG_LEVEL` | string | `info` | Logging level |
| `ENABLE_CONSOLE_LOGGING` | boolean | `true` | Enable console logging |
| `ENABLE_REMOTE_LOGGING` | boolean | `false` | Enable remote logging |

### Docker Configuration

```dockerfile
# Set environment variables in Dockerfile
ENV API_BASE_URL=https://api.yourapp.com
ENV CHUNK_SIZE=10485760
ENV MAX_FILE_SIZE=21474836480
ENV LOG_LEVEL=warn
ENV ENABLE_CONSOLE_LOGGING=false
ENV ENABLE_REMOTE_LOGGING=true
```

### Kubernetes Configuration

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: upload-frontend-config
data:
  API_BASE_URL: "https://api.yourapp.com"
  CHUNK_SIZE: "10485760"
  MAX_FILE_SIZE: "21474836480"
  LOG_LEVEL: "warn"
  ENABLE_CONSOLE_LOGGING: "false"
  ENABLE_REMOTE_LOGGING: "true"
```

---

For more information, see the main [README.md](README.md) file.
