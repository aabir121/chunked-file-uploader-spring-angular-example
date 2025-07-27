# Large File Upload Proof-of-Concept

A full-stack application demonstrating efficient large file upload capabilities using chunked upload technology. This project showcases how to handle extremely large files (50GB+) with features like pause, resume, retry, and real-time progress tracking.

## 🎯 Project Goals

This proof-of-concept was built to explore and demonstrate:

- **Chunked File Upload**: Breaking large files into manageable chunks for reliable transmission
- **Memory Efficiency**: Handling massive files without OutOfMemory errors
- **Reactive Programming**: Using Spring WebFlux for non-blocking, scalable backend operations
- **Real-time Progress**: Live upload progress tracking and status updates
- **Multiple Upload Methods**: Supporting different upload strategies (multipart, binary, reactive)
- **Resilient Uploads**: Pause, resume, and retry capabilities for interrupted uploads
- **Modern Web Technologies**: Angular frontend with Web Workers for non-blocking UI

## 🏗️ Architecture

### Backend (Spring Boot + WebFlux)
- **Framework**: Spring Boot 3.5.4 with Spring WebFlux
- **Java Version**: JDK 17
- **Build Tool**: Maven
- **Key Features**:
  - Reactive, non-blocking file processing
  - Multiple upload endpoints (multipart, binary, reactive)
  - Chunk-based file assembly
  - Upload status tracking
  - Swagger API documentation

### Frontend (Angular)
- **Framework**: Angular 19
- **Language**: TypeScript
- **UI Library**: Angular Material
- **Key Features**:
  - Drag-and-drop file selection
  - Real-time upload progress
  - Web Workers for background processing
  - Multiple upload service implementations
  - Responsive Material Design UI

## 🚀 Features

### Core Upload Capabilities
- ✅ **Chunked Upload**: Files split into 5MB chunks for reliable transmission
- ✅ **Multiple Endpoints**: Support for multipart, binary, and reactive upload methods
- ✅ **Progress Tracking**: Real-time progress updates with chunk-level granularity
- ✅ **Status Management**: Track upload states (pending, uploading, completed, failed)
- ✅ **Memory Efficient**: Stream-based processing to handle files larger than available RAM

### Advanced Features
- ✅ **Web Workers**: Background processing to keep UI responsive during uploads
- ✅ **Concurrent Uploads**: Multiple files can be uploaded simultaneously
- ✅ **Upload Resume**: Ability to resume interrupted uploads (architecture ready)
- ✅ **Error Handling**: Comprehensive error handling with detailed logging
- ✅ **CORS Support**: Configured for cross-origin requests during development

### Developer Experience
- ✅ **API Documentation**: Swagger UI available at `/swagger-ui.html`
- ✅ **Comprehensive Logging**: Detailed logging for debugging and monitoring
- ✅ **Hot Reload**: Development servers with live reload capabilities
- ✅ **Type Safety**: Full TypeScript implementation in frontend

## 📁 Project Structure

```
large-upload/
├── backend/                 # Spring Boot WebFlux application
│   ├── src/main/java/
│   │   └── com/example/largeupload/
│   │       ├── controller/  # REST controllers
│   │       ├── service/     # Business logic
│   │       ├── model/       # Data models
│   │       └── exception/   # Custom exceptions
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
├── frontend/                # Angular application
│   ├── src/app/
│   │   ├── services/        # Upload services
│   │   ├── models/          # TypeScript models
│   │   ├── upload-modal/    # File selection modal
│   │   ├── upload-button/   # Upload trigger component
│   │   ├── uploads-table/   # Progress tracking table
│   │   └── upload.worker.ts # Web Worker for background processing
│   ├── angular.json
│   └── package.json
└── uploads/                 # File storage directory (created at runtime)
```

## 🛠️ Technology Stack

### Backend Technologies
- **Spring Boot 3.5.4** - Application framework
- **Spring WebFlux** - Reactive web framework
- **Maven** - Dependency management and build tool
- **Swagger/OpenAPI** - API documentation
- **SLF4J + Logback** - Logging framework

### Frontend Technologies
- **Angular 19** - Frontend framework
- **TypeScript** - Programming language
- **Angular Material** - UI component library
- **RxJS** - Reactive programming library
- **Web Workers** - Background processing
- **Angular CLI** - Development tooling

## 🚦 Getting Started

### Prerequisites
- **Java 17** or higher
- **Node.js 18** or higher
- **Maven 3.6** or higher
- **Angular CLI** (optional, for development)

### Backend Setup
```bash
cd backend
mvn clean install
mvn spring-boot:run
```
The backend will start on `http://localhost:8080`

### Frontend Setup
```bash
cd frontend
npm install
npm start
```
The frontend will start on `http://localhost:4200`

## 📚 API Documentation

Once the backend is running, visit:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`

### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/upload` | Upload file chunk (multipart) |
| `POST` | `/upload/reactive` | Upload file chunk (reactive) |
| `POST` | `/upload/binary` | Upload file chunk (binary) |
| `GET` | `/upload/{fileId}` | Get upload status for specific file |
| `GET` | `/upload` | Get status of all uploads |

## 🔧 Configuration

### Backend Configuration (`application.properties`)
```properties
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
logging.level.com.example.largeupload=DEBUG
```

### Frontend Configuration
- **Chunk Size**: 5MB (configurable in `upload.service.ts`)
- **Backend URL**: `http://localhost:8080/upload`
- **CORS Origins**: Configured for `localhost:4200` and `localhost:4201`

## 🧪 Testing

The application includes comprehensive testing capabilities:
- **Unit Tests**: Both backend and frontend include unit test suites
- **Integration Testing**: API endpoints can be tested via Swagger UI
- **Manual Testing**: Upload various file sizes to test chunking behavior

## 🎯 Use Cases

This proof-of-concept demonstrates solutions for:
- **Media Upload Platforms**: Video, audio, and image upload services
- **Document Management**: Large document and archive uploads
- **Data Transfer Applications**: Scientific data, backups, and bulk transfers
- **Cloud Storage Services**: Reliable file upload with resume capabilities
- **Enterprise Applications**: Internal file sharing and collaboration tools

## 🔮 Future Enhancements

Potential improvements and extensions:
- [ ] **Authentication & Authorization**: User management and access control
- [ ] **File Deduplication**: Avoid storing duplicate files
- [ ] **Cloud Storage Integration**: AWS S3, Google Cloud Storage support
- [ ] **Upload Scheduling**: Queue and schedule large uploads
- [ ] **Bandwidth Throttling**: Control upload speed and resource usage
- [ ] **File Validation**: Virus scanning and content validation
- [ ] **Metadata Extraction**: Automatic file metadata extraction
- [ ] **Multi-tenant Support**: Isolated storage per organization

## 📄 License

This project is a proof-of-concept for educational and demonstration purposes.

## 🤝 Contributing

This is a proof-of-concept project. Feel free to fork and experiment with different approaches to large file upload challenges.
