# Large File Upload Frontend

A robust, enterprise-grade Angular application for handling large file uploads with chunked upload support, progress tracking, and comprehensive error handling.

## 🚀 Features

### Core Upload Features
- **Chunked Upload**: Large files are split into configurable chunks for reliable upload
- **Resume Support**: Pause and resume uploads at any time
- **Progress Tracking**: Real-time progress updates with ETA calculations
- **Multiple Upload Strategies**: Support for both multipart and binary upload methods
- **File Validation**: Comprehensive client-side file validation
- **Error Recovery**: Automatic retry with exponential backoff

### User Experience
- **Drag & Drop**: Intuitive file selection interface
- **Real-time Notifications**: Toast notifications for upload events
- **Responsive Design**: Works seamlessly on desktop and mobile
- **Upload Queue Management**: View, control, and monitor multiple uploads
- **File Type Detection**: Automatic file type recognition with icons

### Technical Features
- **Web Workers**: Non-blocking uploads using dedicated worker threads
- **Configuration Management**: Environment-based configuration system
- **Comprehensive Logging**: Structured logging with configurable levels
- **Error Handling**: Centralized error handling with user-friendly messages
- **TypeScript**: Full type safety throughout the application
- **Modular Architecture**: Clean, maintainable code structure

## 🏗️ Architecture

### Project Structure
```
src/
├── app/
│   ├── core/                          # Core services and utilities
│   │   ├── config/                    # Configuration management
│   │   ├── error-handling/            # Error handling services
│   │   ├── interfaces/                # TypeScript interfaces
│   │   ├── logging/                   # Logging services
│   │   └── upload-strategies/         # Upload strategy implementations
│   ├── shared/                        # Shared components and utilities
│   │   ├── components/                # Reusable UI components
│   │   ├── constants/                 # Application constants
│   │   └── utils/                     # Utility functions
│   ├── models/                        # Data models and interfaces
│   ├── services/                      # Legacy services (being refactored)
│   ├── components/                    # Feature components
│   └── environments/                  # Environment configurations
```

### Key Components

#### Upload Service (`upload.service.ts`)
- Central service managing all upload operations
- Handles file validation, chunking, and worker communication
- Integrates with error handling and notification systems

#### Upload Strategies
- **MultipartUploadStrategy**: Traditional form-data uploads
- **BinaryUploadStrategy**: Raw binary uploads with metadata headers

#### Configuration System
- Environment-based configuration with fallbacks
- Runtime configuration validation
- Support for environment variables in production

#### Error Handling
- Centralized error handling with severity levels
- User-friendly error messages
- Automatic error recovery strategies

## 🛠️ Setup and Installation

### Prerequisites
- Node.js 18+ and npm
- Angular CLI 19+

### Installation
```bash
# Clone the repository
git clone <repository-url>
cd large-upload-frontend

# Install dependencies
npm install

# Start development server
ng serve
```

The application will be available at `http://localhost:4200/`

### Building for Production
```bash
# Build for production
ng build --configuration production

# The build artifacts will be stored in the `dist/` directory
```

## ⚙️ Configuration

### Environment Variables

The application supports extensive configuration through environment variables:

#### API Configuration
- `API_BASE_URL`: Backend API base URL (default: `http://localhost:8080`)
- `UPLOAD_ENDPOINT`: Upload endpoint path (default: `/upload`)
- `BINARY_UPLOAD_ENDPOINT`: Binary upload endpoint (default: `/upload/binary`)
- `STATUS_ENDPOINT`: Status check endpoint (default: `/upload/status`)

#### Upload Configuration
- `CHUNK_SIZE`: Upload chunk size in bytes (default: `5242880` - 5MB)
- `MAX_CONCURRENT_UPLOADS`: Maximum concurrent uploads (default: `3`)
- `RETRY_ATTEMPTS`: Number of retry attempts (default: `3`)
- `RETRY_DELAY`: Base retry delay in milliseconds (default: `1000`)
- `TIMEOUT_MS`: Request timeout in milliseconds (default: `30000`)

#### UI Configuration
- `PROGRESS_UPDATE_INTERVAL`: Progress update frequency (default: `100`ms)
- `AUTO_CLOSE_MODAL_DELAY`: Modal auto-close delay (default: `2000`ms)
- `MAX_FILE_SIZE`: Maximum file size in bytes (default: `10737418240` - 10GB)
- `ALLOWED_FILE_TYPES`: Comma-separated list of allowed MIME types (default: `*`)

#### Logging Configuration
- `LOG_LEVEL`: Logging level (`error`, `warn`, `info`, `debug`) (default: `info`)
- `ENABLE_CONSOLE_LOGGING`: Enable console logging (default: `true`)
- `ENABLE_REMOTE_LOGGING`: Enable remote logging (default: `false`)

### Development Configuration

For development, edit `src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  api: {
    baseUrl: 'http://localhost:8080',
    uploadEndpoint: '/upload',
    // ... other settings
  },
  upload: {
    chunkSize: 5 * 1024 * 1024, // 5MB
    maxConcurrentUploads: 3,
    // ... other settings
  }
  // ... other configuration sections
};
```

## 📖 Usage Guide

### Basic Upload Flow

1. **File Selection**: Click the "Upload File" button or drag files to the upload area
2. **File Validation**: Files are automatically validated against size and type restrictions
3. **Upload Progress**: Monitor upload progress with real-time updates
4. **Upload Control**: Pause, resume, or cancel uploads as needed
5. **Completion**: Receive notifications when uploads complete or fail

### Upload Controls

#### Upload Actions
- **Pause**: Temporarily stop an upload (can be resumed later)
- **Resume**: Continue a paused or failed upload
- **Cancel**: Permanently stop and remove an upload
- **Retry**: Restart a failed upload
- **Remove**: Remove completed/failed uploads from the list

#### Progress Information
- **Progress Bar**: Visual progress indicator with percentage
- **Chunk Progress**: Shows completed chunks vs. total chunks
- **Upload Speed**: Real-time upload speed (when available)
- **ETA**: Estimated time to completion
- **Status**: Current upload status (pending, uploading, paused, etc.)

### File Validation

The application performs comprehensive file validation:

- **File Size**: Configurable maximum file size limit
- **File Type**: MIME type validation against allowed types
- **File Name**: Security checks for malicious file names
- **File Content**: Basic content validation

### Error Handling

The application provides robust error handling:

- **Network Errors**: Automatic retry with exponential backoff
- **Server Errors**: User-friendly error messages with suggested actions
- **Validation Errors**: Clear feedback on why files were rejected
- **Recovery Options**: Retry buttons and alternative actions

## 🔧 Development

### Code Structure

#### Services
- **UploadService**: Main upload orchestration
- **AppConfigService**: Configuration management
- **LoggerService**: Structured logging
- **NotificationService**: User notifications
- **AppErrorHandler**: Centralized error handling

#### Components
- **UploadButtonComponent**: File selection trigger
- **UploadModalComponent**: File selection modal
- **UploadsTableComponent**: Upload queue display
- **ProgressBarComponent**: Reusable progress indicator
- **FileInfoComponent**: File information display
- **UploadActionsComponent**: Upload control buttons
- **NotificationsComponent**: Toast notifications

#### Utilities
- **FileUtils**: File manipulation and validation utilities
- **UploadUtils**: Upload-specific utility functions
- **ValidationUtils**: General validation utilities

### Adding New Upload Strategies

To add a new upload strategy:

1. Create a new strategy class implementing `IUploadStrategy`:

```typescript
@Injectable({
  providedIn: 'root'
})
export class CustomUploadStrategy implements IUploadStrategy {
  uploadChunk(chunk: UploadChunk, uploadUrl: string, signal?: AbortSignal): Observable<any> {
    // Implementation
  }

  getName(): string {
    return 'Custom Upload';
  }

  getDescription(): string {
    return 'Custom upload strategy description';
  }
}
```

2. Register the strategy in your upload service
3. Update configuration to support the new strategy

### Extending Configuration

To add new configuration options:

1. Update the environment files (`environment.ts`, `environment.prod.ts`)
2. Update the `AppConfigService` interfaces and methods
3. Use the new configuration in your components/services

### Custom Validation

To add custom file validation:

1. Extend the `UploadValidator` class
2. Add new validation methods
3. Update the validation pipeline in `UploadService`

## 🧪 Testing

### Running Tests

```bash
# Run unit tests
ng test

# Run tests with coverage
ng test --code-coverage

# Run tests in watch mode
ng test --watch

# Run e2e tests (if configured)
ng e2e
```

### Test Structure

- **Unit Tests**: Component and service testing with Jasmine/Karma
- **Integration Tests**: Testing component interactions
- **E2E Tests**: End-to-end user workflow testing

### Writing Tests

Example service test:

```typescript
describe('UploadService', () => {
  let service: UploadService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [UploadService]
    });
    service = TestBed.inject(UploadService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should upload file successfully', () => {
    const mockFile = new File(['test'], 'test.txt', { type: 'text/plain' });
    service.uploadFile(mockFile);

    // Test expectations
  });
});
```

## 🚀 Deployment

### Production Build

```bash
# Create production build
ng build --configuration production

# Build with specific environment
ng build --configuration staging
```

### Environment-Specific Builds

The application supports multiple environments:

- **Development**: `ng serve` or `ng build`
- **Production**: `ng build --configuration production`
- **Staging**: `ng build --configuration staging` (if configured)

### Docker Deployment

Example Dockerfile:

```dockerfile
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

COPY . .
RUN npm run build --prod

FROM nginx:alpine
COPY --from=builder /app/dist/frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### Environment Variables in Production

Set environment variables in your deployment environment:

```bash
export API_BASE_URL=https://api.yourapp.com
export CHUNK_SIZE=10485760
export MAX_FILE_SIZE=21474836480
export LOG_LEVEL=warn
```

## 🔍 Monitoring and Debugging

### Logging

The application provides comprehensive logging:

- **Console Logging**: Development and debugging
- **Remote Logging**: Production monitoring (configurable)
- **Error Tracking**: Centralized error collection
- **Performance Metrics**: Upload performance tracking

### Debug Mode

Enable debug logging in development:

```typescript
// environment.ts
logging: {
  level: 'debug',
  enableConsoleLogging: true
}
```

### Performance Monitoring

Monitor upload performance:

- **Upload Speed**: Track transfer rates
- **Error Rates**: Monitor failure rates
- **User Experience**: Track completion times
- **Resource Usage**: Monitor memory and CPU usage

## 🤝 Contributing

### Development Workflow

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/new-feature`
3. Make changes and add tests
4. Run tests: `npm test`
5. Commit changes: `git commit -m "Add new feature"`
6. Push to branch: `git push origin feature/new-feature`
7. Create a Pull Request

### Code Standards

- **TypeScript**: Strict type checking enabled
- **ESLint**: Code linting and formatting
- **Prettier**: Code formatting
- **Angular Style Guide**: Follow official Angular conventions
- **Testing**: Maintain test coverage above 80%

### Pull Request Guidelines

- Include tests for new features
- Update documentation as needed
- Follow conventional commit messages
- Ensure all CI checks pass

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

### Common Issues

#### Upload Fails Immediately
- Check file size limits
- Verify file type restrictions
- Check network connectivity
- Review browser console for errors

#### Slow Upload Speeds
- Adjust chunk size configuration
- Check network bandwidth
- Verify server processing capacity
- Consider concurrent upload limits

#### Memory Issues
- Reduce chunk size for large files
- Limit concurrent uploads
- Check browser memory usage
- Consider file size limits

### Getting Help

- **Documentation**: Check this README and inline code comments
- **Issues**: Create GitHub issues for bugs and feature requests
- **Discussions**: Use GitHub Discussions for questions
- **Support**: Contact the development team

### Troubleshooting

Enable debug logging and check browser console:

```typescript
// Temporary debug configuration
logging: {
  level: 'debug',
  enableConsoleLogging: true
}
```

Check network tab in browser dev tools for failed requests.
Review application logs for error details.

---

Built with ❤️ using Angular 19 and TypeScript
