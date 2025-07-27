# Large File Upload Service

A production-ready backend service for uploading extremely large files (50GB+) using chunked uploads. Built with Spring WebFlux and JDK 17, featuring reactive programming, comprehensive validation, and robust error handling.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)
- [Performance Considerations](#performance-considerations)

## Features

### Core Functionality
- **Chunked file upload**: Files are uploaded in configurable chunks, enabling pause, resume, and retry capabilities
- **Reactive processing**: Built on Spring WebFlux for non-blocking, high-performance operations
- **Multiple upload methods**: Support for multipart form data, reactive streams, and binary uploads
- **Efficient storage**: Chunks are immediately persisted to disk with streaming assembly to avoid memory issues
- **Upload status tracking**: Real-time tracking of upload progress with detailed status information
- **Automatic file assembly**: Intelligent chunk combination with integrity validation
- **Concurrent uploads**: Support for multiple simultaneous file uploads

### Reliability & Validation
- **Comprehensive validation**: File size limits, chunk validation, filename sanitization, and extension filtering
- **Error handling**: Detailed error responses with structured error codes and field-level validation
- **Data integrity**: Chunk verification and file assembly validation
- **Cleanup mechanisms**: Automatic cleanup of temporary files and failed uploads
- **Configurable limits**: Customizable file size, chunk size, and concurrency limits

### Monitoring & Operations
- **Upload statistics**: Detailed metrics on upload performance and status
- **Structured logging**: Comprehensive logging with trace IDs for debugging
- **Health monitoring**: Built-in health checks and metrics
- **OpenAPI documentation**: Complete API documentation with Swagger UI

## Architecture

### Service Layer Architecture

The application follows a clean architecture pattern with separated concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                    FileUploadController                     │
│  ┌─────────────────┬─────────────────┬─────────────────┐   │
│  │   Multipart     │    Reactive     │     Binary      │   │
│  │    Upload       │     Upload      │     Upload      │   │
│  └─────────────────┴─────────────────┴─────────────────┘   │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                 FileStorageService                          │
│              (Orchestration Layer)                         │
└─────┬─────────────┬─────────────┬─────────────┬─────────────┘
      │             │             │             │
┌─────▼─────┐ ┌─────▼─────┐ ┌─────▼─────┐ ┌─────▼─────┐
│   Chunk   │ │   File    │ │  Upload   │ │  Upload   │
│ Storage   │ │ Assembly  │ │  Status   │ │Validation │
│ Service   │ │ Service   │ │ Service   │ │ Service   │
└───────────┘ └───────────┘ └───────────┘ └───────────┘
```

### Key Components

1. **FileUploadController**: REST API endpoints with reactive support
2. **FileStorageService**: Main orchestration service coordinating all operations
3. **ChunkStorageService**: Handles chunk persistence and temporary file management
4. **FileAssemblyService**: Manages chunk combination and final file creation
5. **UploadStatusService**: Tracks upload progress and maintains state
6. **UploadValidationService**: Validates requests, files, and enforces business rules

### Data Flow

1. **Chunk Upload**: Client uploads file chunks via REST API
2. **Validation**: Request parameters and chunk data are validated
3. **Storage**: Chunks are stored in temporary directories
4. **Status Tracking**: Upload progress is updated in memory
5. **Assembly**: When all chunks are received, they're combined into the final file
6. **Cleanup**: Temporary files are removed after successful assembly

## Quick Start

### Prerequisites
- JDK 17 or higher
- Maven 3.6+
- 2GB+ available disk space for uploads

### Build and Run

```bash
# Clone the repository
git clone <repository-url>
cd large-upload/backend

# Build the application
mvn clean install

# Run with default configuration
mvn spring-boot:run

# Or run the JAR directly
java -jar target/large-upload-0.0.1-SNAPSHOT.jar
```

The service will start on `http://localhost:8080` with Swagger UI available at `http://localhost:8080/swagger-ui.html`.

## Configuration

### Application Properties

The service is highly configurable through `application.properties`:

```properties
# File Upload Configuration
file-upload.storage.base-directory=uploads
file-upload.storage.temp-directory-prefix=temp_

# Chunk Configuration
file-upload.chunk.default-size=5242880          # 5MB default chunk size
file-upload.chunk.max-size=104857600             # 100MB max chunk size
file-upload.chunk.max-count=10000                # Maximum chunks per file

# File Size Limits
file-upload.file.max-size=53687091200            # 50GB max file size

# Cleanup Configuration
file-upload.cleanup.auto-cleanup-enabled=true
file-upload.cleanup.cleanup-delay-hours=24

# Validation Configuration
file-upload.validation.allowed-extensions=       # Empty = all allowed except blocked
file-upload.validation.blocked-extensions=exe,bat,cmd,scr,com,pif

# Performance Configuration
file-upload.performance.max-concurrent-uploads=10
file-upload.performance.io-thread-pool-size=4

# Spring WebFlux Configuration
spring.webflux.multipart.max-in-memory-size=10MB
spring.webflux.multipart.max-disk-usage-per-part=100MB
spring.webflux.multipart.max-parts=128
```

### Environment-Specific Configuration

#### Development
```properties
# Development settings
file-upload.storage.base-directory=./dev-uploads
file-upload.file.max-size=1073741824            # 1GB for development
logging.level.com.example.largeupload=DEBUG
```

#### Production
```properties
# Production settings
file-upload.storage.base-directory=/var/uploads
file-upload.file.max-size=107374182400          # 100GB for production
file-upload.performance.max-concurrent-uploads=50
file-upload.performance.io-thread-pool-size=16
logging.level.com.example.largeupload=INFO
```

### Configuration Properties Reference

| Property | Default | Description |
|----------|---------|-------------|
| `file-upload.storage.base-directory` | `uploads` | Base directory for file storage |
| `file-upload.storage.temp-directory-prefix` | `temp_` | Prefix for temporary directories |
| `file-upload.chunk.default-size` | `5242880` | Default chunk size in bytes (5MB) |
| `file-upload.chunk.max-size` | `104857600` | Maximum chunk size in bytes (100MB) |
| `file-upload.chunk.max-count` | `10000` | Maximum number of chunks per file |
| `file-upload.file.max-size` | `53687091200` | Maximum file size in bytes (50GB) |
| `file-upload.cleanup.auto-cleanup-enabled` | `true` | Enable automatic cleanup of old uploads |
| `file-upload.cleanup.cleanup-delay-hours` | `24` | Hours to wait before cleaning up completed uploads |
| `file-upload.validation.allowed-extensions` | `` | Comma-separated list of allowed file extensions |
| `file-upload.validation.blocked-extensions` | `exe,bat,cmd,scr,com,pif` | Comma-separated list of blocked file extensions |
| `file-upload.performance.max-concurrent-uploads` | `10` | Maximum concurrent uploads |
| `file-upload.performance.io-thread-pool-size` | `4` | Thread pool size for I/O operations |

## API Documentation

### Interactive Documentation
Complete API documentation is available via Swagger UI at `http://localhost:8080/swagger-ui.html` when the service is running.

### Core Endpoints

#### Upload File Chunk
```http
POST /upload
Content-Type: multipart/form-data

Parameters:
- fileId (string): Unique identifier for the file
- chunkNumber (string): The chunk number (0-based)
- totalChunks (string): Total number of chunks for the file
- fileName (string): Original filename
- file (file): The file chunk data
```

**Example using curl:**
```bash
curl -X POST "http://localhost:8080/upload" \
  -F "fileId=unique-file-123" \
  -F "chunkNumber=0" \
  -F "totalChunks=5" \
  -F "fileName=document.pdf" \
  -F "file=@chunk-0.dat"
```

#### Binary Upload (Alternative)
```http
POST /upload/binary
Content-Type: application/octet-stream
X-File-Id: unique-file-123
X-Chunk-Number: 0
X-Total-Chunks: 5
X-File-Name: document.pdf

[Binary chunk data]
```

#### Get Upload Status
```http
GET /upload/{fileId}
```

**Response:**
```json
{
  "fileId": "unique-file-123",
  "totalChunks": 5,
  "receivedChunks": [0, 1, 2],
  "fileName": "document.pdf",
  "complete": false,
  "failed": false,
  "createdAt": "2024-01-15T10:30:00Z",
  "lastUpdated": "2024-01-15T10:35:00Z"
}
```

#### Complete Upload
```http
POST /upload/{fileId}/complete
```

#### Get All Upload Statuses
```http
GET /upload
```

#### Test Endpoint
```http
POST /upload/test
```

### Upload Workflow

1. **Initialize Upload**: Generate a unique `fileId` for your file
2. **Calculate Chunks**: Divide your file into chunks (recommended: 5-10MB per chunk)
3. **Upload Chunks**: Send each chunk with its metadata
4. **Monitor Progress**: Check upload status as needed
5. **Complete Upload**: Trigger file assembly when all chunks are uploaded
6. **Verify Result**: Check that the final file was created successfully

### Error Responses

All endpoints return structured error responses:

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Validation Error",
  "message": "Validation failed",
  "path": "/upload",
  "errorCode": "VALIDATION_ERROR",
  "details": {
    "field": "chunkNumber",
    "rejectedValue": "invalid"
  },
  "traceId": "abc123"
}
```

### Common Error Codes

| Code | Description |
|------|-------------|
| `VALIDATION_ERROR` | Request validation failed |
| `CHUNK_WRITE_FAILED` | Failed to save chunk to disk |
| `ASSEMBLY_FAILED` | Failed to assemble final file |
| `MISSING_CHUNKS` | Not all chunks available for assembly |
| `FILE_TOO_LARGE` | File exceeds maximum size limit |
| `CHUNK_TOO_LARGE` | Chunk exceeds maximum size limit |
| `INVALID_EXTENSION` | File extension not allowed |

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run only unit tests
mvn test -Dtest="*Test"

# Run only integration tests
mvn test -Dtest="*IntegrationTest"

# Run performance tests (requires system property)
mvn test -Drun.performance.tests=true -Dtest="*PerformanceTest"
```

### Test Categories

#### Unit Tests
- **Service Layer Tests**: Comprehensive testing of all service classes
- **Validation Tests**: Testing of all validation rules and edge cases
- **Error Handling Tests**: Testing of exception scenarios and error responses

#### Integration Tests
- **Complete Upload Workflow**: End-to-end testing of file upload process
- **Concurrent Upload Testing**: Testing multiple simultaneous uploads
- **Error Scenario Testing**: Testing various failure conditions
- **API Endpoint Testing**: Testing all REST endpoints

#### Performance Tests
- **Large File Upload**: Testing with 50MB+ files
- **Concurrent Load Testing**: Testing with multiple simultaneous uploads
- **Memory Usage Testing**: Ensuring reasonable memory consumption
- **Throughput Testing**: Measuring upload performance

### Test Configuration

Performance tests are disabled by default. Enable them with:
```bash
-Drun.performance.tests=true
```

## Deployment

### Docker Deployment

Create a `Dockerfile`:
```dockerfile
FROM openjdk:17-jre-slim

WORKDIR /app
COPY target/large-upload-0.0.1-SNAPSHOT.jar app.jar
COPY application-prod.properties application.properties

# Create upload directory
RUN mkdir -p /var/uploads && chmod 755 /var/uploads

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:
```bash
docker build -t large-upload-service .
docker run -p 8080:8080 -v /host/uploads:/var/uploads large-upload-service
```

### Production Considerations

#### System Requirements
- **CPU**: 4+ cores recommended for high concurrency
- **Memory**: 2GB+ heap space, additional memory for OS file caching
- **Storage**: Fast SSD storage recommended, space for temporary and final files
- **Network**: High bandwidth for large file transfers

#### JVM Configuration
```bash
java -Xmx4g -Xms2g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+UseStringDeduplication \
     -jar large-upload-service.jar
```

#### Monitoring
- Monitor disk space in upload directories
- Track memory usage and GC performance
- Monitor upload success/failure rates
- Set up alerts for service health

## Troubleshooting

### Common Issues

#### Upload Fails with "Chunk too large" Error
**Cause**: Chunk size exceeds configured maximum
**Solution**:
- Reduce chunk size in client
- Increase `file-upload.chunk.max-size` in configuration
- Check `spring.webflux.multipart.max-disk-usage-per-part` setting

#### "Missing chunks" Error During Assembly
**Cause**: Not all chunks were successfully uploaded
**Solution**:
- Check upload status to identify missing chunks: `GET /upload/{fileId}`
- Re-upload missing chunks
- Verify chunk numbering starts from 0

#### Out of Memory Errors
**Cause**: Insufficient heap space or memory leaks
**Solution**:
- Increase JVM heap size: `-Xmx4g`
- Check for memory leaks in upload status tracking
- Reduce concurrent upload limit
- Enable automatic cleanup of old uploads

#### Disk Space Issues
**Cause**: Insufficient disk space for temporary or final files
**Solution**:
- Monitor disk usage in upload directories
- Implement disk space checks before accepting uploads
- Clean up old temporary directories manually if needed

#### Slow Upload Performance
**Cause**: Various performance bottlenecks
**Solution**:
- Use SSD storage for upload directories
- Increase I/O thread pool size
- Optimize chunk size (5-10MB recommended)
- Check network bandwidth and latency

### Debugging

#### Enable Debug Logging
```properties
logging.level.com.example.largeupload=DEBUG
logging.level.org.springframework.web=DEBUG
```

#### Check Upload Status
```bash
# Get status for specific upload
curl http://localhost:8080/upload/{fileId}

# Get all upload statuses
curl http://localhost:8080/upload
```

#### Monitor Temporary Directories
```bash
# Check temporary directories
ls -la uploads/temp_*

# Check disk usage
du -sh uploads/
```

#### Health Check
```bash
# Test service health
curl http://localhost:8080/upload/test
```

### Log Analysis

#### Key Log Messages
- `"Chunk saved successfully"`: Successful chunk upload
- `"File assembly completed"`: Successful file assembly
- `"Cleaned up temporary directory"`: Successful cleanup
- `"Validation failed"`: Request validation errors
- `"Failed to save chunk"`: Storage errors

#### Trace ID Tracking
All requests include trace IDs for correlation across log entries. Search logs by trace ID to follow a complete request flow.

## Performance Considerations

### Optimal Configuration

#### Chunk Size Recommendations
- **Small files (< 100MB)**: 1-5MB chunks
- **Medium files (100MB - 1GB)**: 5-10MB chunks
- **Large files (> 1GB)**: 10-50MB chunks
- **Very large files (> 10GB)**: 50-100MB chunks

#### Concurrency Settings
- **Development**: 5-10 concurrent uploads
- **Production (4 cores)**: 20-50 concurrent uploads
- **Production (8+ cores)**: 50-100 concurrent uploads

#### Memory Allocation
- **Minimum**: 2GB heap space
- **Recommended**: 4-8GB heap space for high load
- **Additional**: 2-4GB for OS file caching

### Performance Tuning

#### JVM Tuning
```bash
# G1 GC for low latency
-XX:+UseG1GC -XX:MaxGCPauseMillis=200

# String deduplication for memory efficiency
-XX:+UseStringDeduplication

# Large pages for better memory performance
-XX:+UseLargePages

# Optimize for throughput
-XX:+AggressiveOpts
```

#### Application Tuning
```properties
# Increase thread pools
file-upload.performance.io-thread-pool-size=16
file-upload.performance.max-concurrent-uploads=50

# Optimize WebFlux
spring.webflux.multipart.max-in-memory-size=50MB
spring.webflux.multipart.streaming=true

# Enable compression
server.compression.enabled=true
server.compression.mime-types=application/json,text/plain
```

#### Storage Optimization
- Use SSD storage for upload directories
- Separate temporary and final storage if possible
- Consider RAID configuration for redundancy
- Monitor I/O wait times and disk queue depth

### Monitoring Metrics

#### Key Performance Indicators
- **Upload throughput**: MB/s per upload
- **Concurrent uploads**: Number of simultaneous uploads
- **Success rate**: Percentage of successful uploads
- **Average assembly time**: Time to combine chunks
- **Memory usage**: Heap and off-heap memory consumption
- **Disk usage**: Storage consumption and growth rate

#### Alerting Thresholds
- Memory usage > 80% of allocated heap
- Disk usage > 85% of available space
- Upload success rate < 95%
- Average response time > 5 seconds
- Error rate > 1% of total requests

---

## Under the Hood: Technical Implementation

### Chunk Storage Mechanism

When a chunk is uploaded:

1. **Validation**: Request parameters and chunk data are validated
2. **Temporary Storage**: Chunk is written to `uploads/temp_{fileId}/{fileId}.part{chunkNumber}`
3. **Status Update**: Upload status is updated in memory with received chunk information
4. **Response**: Success response is returned to client

### File Assembly Process

When all chunks are received and assembly is triggered:

1. **Verification**: Confirm all chunks (0 to totalChunks-1) are present
2. **Sequential Reading**: Chunks are read in order using NIO FileChannel
3. **Streaming Write**: Data is streamed to final file location to avoid memory issues
4. **Integrity Check**: Final file size is verified against expected size
5. **Cleanup**: Temporary directory and chunks are removed
6. **Status Update**: Upload is marked as completed

### Memory Management

The service is designed to handle large files without loading them entirely into memory:

- **Streaming Processing**: Chunks are processed as streams, not loaded into memory
- **Bounded Memory**: Upload status tracking uses fixed-size data structures
- **Automatic Cleanup**: Temporary files are cleaned up to prevent disk space leaks
- **Reactive Streams**: Non-blocking I/O prevents thread pool exhaustion

### Concurrency Handling

- **Thread-Safe Operations**: All services use thread-safe data structures
- **Reactive Programming**: Non-blocking operations using Project Reactor
- **Bounded Resources**: Configurable limits on concurrent uploads and thread pools
- **Isolation**: Each upload uses separate temporary directories

This architecture ensures the service can handle multiple large file uploads simultaneously while maintaining reasonable resource usage and providing reliable operation.
