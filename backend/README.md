# Large File Upload Proof-of-Concept

This project is a proof-of-concept for a backend service that supports uploading extremely large files (50GB+) using chunked uploads. It is built with Spring WebFlux and JDK 17.

## Features

*   **Chunked file upload:** Files are uploaded in small chunks, allowing for pause, resume, and retry capabilities.
*   **Efficient storage:** Chunks are immediately persisted to disk, and the final file is assembled using file streams to avoid OutOfMemory errors.
*   **Upload status tracking:** API endpoints are provided to track the status of ongoing and finished uploads.
*   **No security:** CORS restrictions are disabled, and no authentication or access control is implemented for ease of development.

## How to Build and Run

1.  **Prerequisites:**
    *   JDK 17
    *   Maven

2.  **Build the application:**

    ```bash
    mvn clean install
    ```

3.  **Run the application:**

    ```bash
    mvn spring-boot:run
    ```

## API Documentation

API documentation is available via Swagger UI at `http://localhost:8080/swagger-ui.html`.

### Endpoints

*   **`POST /upload`**: Uploads a file chunk.

    *   **Parameters:**
        *   `fileId` (string): Unique identifier for the file.
        *   `chunkNumber` (integer): The chunk number (0-based).
        *   `totalChunks` (integer): Total number of chunks for the file.
        *   `file` (file): The file chunk.

*   **`GET /upload/{fileId}`**: Retrieves the upload status for a specific file.

*   **`GET /upload`**: Retrieves the status of all ongoing file uploads.
