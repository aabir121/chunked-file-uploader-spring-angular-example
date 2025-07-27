# Large File Upload POC with Angular and Spring Boot

This project is a Proof-of-Concept (POC) demonstrating how to handle large file uploads efficiently and reliably in a modern web application using Angular for the frontend and Spring Boot for the backend. The key feature highlighted is the ability to pause and resume uploads, built on a robust chunking mechanism.

**Disclaimer:** This is a proof-of-concept and is **not production-ready**. It lacks crucial features like a persistent state layer and an authentication/authorization layer.

## Motivation

Uploading large files presents several challenges in web development, including network instability, server limitations, and poor user experience due to frozen interfaces. This project was built to explore and showcase a solution to these problems. It serves as a practical guide and a starting point for developers looking to implement a resilient file upload system where users can upload gigabytes of data without fear of a single network glitch forcing them to restart from scratch.

## High-Level Architecture

The system is composed of two main parts: a client-side application (Frontend) and a server-side application (Backend).

### Frontend (Angular)

The frontend is a standalone Angular application that serves as the user interface for the file upload process.

-   **UI/UX:** It provides a clean and intuitive interface for users to select, monitor, and manage their file uploads.
-   **Chunking:** Before an upload begins, the selected file is divided into smaller, manageable chunks. This is the foundation for the pause/resume capability.
-   **Performance:** To ensure the UI remains responsive, the computationally intensive tasks of file slicing are offloaded to a **Web Worker**. This prevents the main browser thread from becoming blocked, even when processing very large files.
-   **Communication:** It communicates with the backend via a RESTful API to send the file chunks and manage the upload lifecycle.

For more details on the frontend implementation, please see the [frontend README](frontend/README.md).

### Backend (Spring Boot)

The backend is a Spring Boot application that exposes a REST API to handle the file upload logic.

-   **Chunk Handling:** It receives the individual file chunks from the frontend.
-   **Reassembly:** It temporarily stores these chunks and, once all chunks for a specific file have been received, it reassembles them into the complete, original file.
-   **State Management:** It maintains the state of each upload, tracking which chunks have been successfully received.

For more details on the backend implementation, please see the [backend README](backend/README.md).

## Future Enhancements

This project serves as a solid foundation, but requires several enhancements for production use:

*   **Persistent State:** Currently, the upload state is stored in memory. For a production environment, this is a critical flaw. This should be replaced with a persistent storage solution like **Redis** or a database to ensure that uploads can be resumed even after a server restart or across multiple server instances.
*   **Authentication & Authorization:** There is currently no security layer. A production system would need a robust authentication and authorization mechanism to control who can upload files and to secure the endpoints.
*   **Scalability:** For very large-scale applications, consider using a distributed storage system like Amazon S3 for storing the final assembled files.
*   **Configuration:** Make the chunk size and other parameters configurable through the application's settings instead of being hardcoded.

## Community and Contributions

This project is open-source and community-driven. We welcome contributions, ideas, and feedback from everyone. If you have suggestions for improvements, new features, or have found a bug, please feel free to open an issue or submit a pull request.

## License

This project is licensed under the MIT License. See the accompanying [LICENSE](LICENSE) file for details.

---
*Note: This project was vibecoded via augment code.*