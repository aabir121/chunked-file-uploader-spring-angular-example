import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, catchError, of, map } from 'rxjs';
import { UploadFile, UploadWorkerMessage, UploadWorkerResponse, BackendUploadStatus, ResumeUploadResponse } from './models/upload.model';
import { v4 as uuidv4 } from 'uuid';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AppConfigService } from './core/config/app-config.service';
import { LoggerService } from './core/logging/logger.service';
import { UploadValidator } from './core/upload-handlers/upload-validator';
import { IUploadService } from './core/interfaces/upload.interfaces';
import { AppErrorHandler } from './core/error-handling/error-handler.service';
import { NotificationService } from './core/notifications/notification.service';
import { BrowserRefreshService } from './services/browser-refresh.service';

@Injectable({
  providedIn: 'root'
})
export class UploadService implements IUploadService {
  private readonly uploadsSubject = new BehaviorSubject<UploadFile[]>([]);
  uploads$: Observable<UploadFile[]>;
  private readonly worker: Worker;
  private readonly logger: any;
  private readonly baseUrl: string;

  constructor(
    private readonly http: HttpClient,
    private readonly configService: AppConfigService,
    private readonly loggerService: LoggerService,
    private readonly validator: UploadValidator,
    private readonly errorHandler: AppErrorHandler,
    private readonly notificationService: NotificationService,
    private readonly browserRefreshService: BrowserRefreshService
  ) {
    this.logger = this.loggerService.createLogger('UploadService');
    this.baseUrl = this.configService.api.baseUrl;
    this.worker = new Worker(new URL('./upload.worker', import.meta.url));
    this.worker.onmessage = ({ data }) => this.handleWorkerMessage(data);
    this.uploads$ = this.uploadsSubject.asObservable();
    this.logger.info('UploadService initialized');

    // Initialize resumable uploads on startup
    this.initializeResumableUploads();

    // Handle browser refresh scenarios
    this.handleBrowserRefresh();
  }

  private handleWorkerMessage(response: UploadWorkerResponse): void {
    const currentUploads = this.uploadsSubject.getValue();
    const uploadIndex = currentUploads.findIndex(u => u.id === response.payload.fileId);

    if (uploadIndex > -1) {
      const updatedUpload = { ...currentUploads[uploadIndex] };

      switch (response.type) {
        case 'progress':
          if (response.payload.progress !== undefined) {
            updatedUpload.progress = response.payload.progress;
            updatedUpload.progressPercentage = response.payload.progress;
          }
          if (response.payload.uploadedChunks) {
            updatedUpload.uploadedChunks = new Set(response.payload.uploadedChunks);
          }
          if (response.payload.uploadedBytes !== undefined) {
            updatedUpload.uploadedBytes = response.payload.uploadedBytes;
          }
          if (response.payload.uploadSpeed !== undefined) {
            updatedUpload.uploadSpeed = response.payload.uploadSpeed;
          }
          if (response.payload.estimatedRemainingTime !== undefined) {
            updatedUpload.estimatedRemainingTime = response.payload.estimatedRemainingTime;
          }
          updatedUpload.lastUpdated = new Date();
          break;
        case 'chunkCompleted':
          if (response.payload.chunkIndex !== undefined) {
            updatedUpload.uploadedChunks.add(response.payload.chunkIndex);
          }
          break;
        case 'completing':
          updatedUpload.status = 'completing';
          updatedUpload.progress = 100; // All chunks uploaded, now finalizing
          break;
        case 'completed':
          updatedUpload.status = 'completed';
          updatedUpload.progress = 100;
          this.handleUploadCompletion(response.payload.fileId, 'completed');
          break;
        case 'failed':
          updatedUpload.status = 'failed';
          if (response.payload.error) {
            updatedUpload.error = response.payload.error;
          }
          this.handleUploadCompletion(response.payload.fileId, 'failed');
          break;
      }
      currentUploads[uploadIndex] = updatedUpload;
      this.uploadsSubject.next([...currentUploads]);
    }
  }

  uploadFile(file: File, uploadUrl?: string, fileId?: string): void {
    this.logger.info('Starting file upload', { fileName: file.name, fileSize: file.size, fileId });

    // Validate file
    const validation = this.validator.validateFile(file);
    if (!validation.valid) {
      this.logger.error('File validation failed', { fileName: file.name, error: validation.error });
      this.errorHandler.handleError(new Error(validation.error || 'File validation failed'), {
        component: 'UploadService',
        action: 'uploadFile'
      });
      this.notificationService.fileValidationError(file.name, validation.error || 'File validation failed');
      return;
    }

    const chunkSize = this.configService.upload.chunkSize;
    const totalChunks = Math.ceil(file.size / chunkSize);
    const finalFileId = fileId || uuidv4();
    const finalUploadUrl = uploadUrl || this.configService.getUploadUrl();

    // Check if this is a resume scenario
    if (fileId) {
      this.resumeUploadFromBackend(finalFileId, totalChunks, file.name, file.size, chunkSize)
        .subscribe({
          next: (resumeResponse) => {
            this.logger.info('Resume response received', { fileId: finalFileId, resumeResponse });
            this.handleResumeResponse(resumeResponse, file, finalUploadUrl);
          },
          error: (error) => {
            this.logger.warn('Resume failed, starting new upload', { fileId: finalFileId, error });
            this.startNewUpload(file, finalFileId, totalChunks, chunkSize, finalUploadUrl);
          }
        });
    } else {
      this.startNewUpload(file, finalFileId, totalChunks, chunkSize, finalUploadUrl);
    }
  }

  private startNewUpload(file: File, fileId: string, totalChunks: number, chunkSize: number, uploadUrl: string): void {
    const newUpload: UploadFile = {
      id: fileId,
      file: file,
      name: file.name,
      size: file.size,
      progress: 0,
      status: 'pending',
      uploadedChunks: new Set<number>(),
      totalChunks: totalChunks,
      chunkSize: chunkSize,
      uploadUrl: uploadUrl,
      // Initialize enhanced tracking fields
      uploadedBytes: 0,
      progressPercentage: 0,
      uploadSpeed: 0,
      estimatedRemainingTime: undefined,
      missingChunks: new Set(Array.from({ length: totalChunks }, (_, i) => i)),
      nextExpectedChunk: 0,
      canResume: true,
      createdAt: new Date(),
      lastUpdated: new Date()
    };

    this.logger.debug('Created upload task', {
      fileId,
      fileName: file.name,
      totalChunks,
      chunkSize,
      uploadUrl
    });

    const currentUploads = this.uploadsSubject.getValue();
    this.uploadsSubject.next([...currentUploads, newUpload]);

    // Register upload for browser refresh detection
    this.browserRefreshService.registerActiveUpload(fileId);

    this.startUpload(newUpload);
  }

  private handleResumeResponse(resumeResponse: ResumeUploadResponse, file: File, uploadUrl: string): void {
    const resumedUpload: UploadFile = {
      id: resumeResponse.fileId,
      file: file,
      name: resumeResponse.fileName || file.name,
      size: resumeResponse.fileSize || file.size,
      progress: resumeResponse.progressPercentage,
      status: this.determineResumeStatus(resumeResponse),
      uploadedChunks: new Set(resumeResponse.receivedChunks),
      totalChunks: resumeResponse.totalChunks,
      chunkSize: resumeResponse.chunkSize || this.configService.upload.chunkSize,
      uploadUrl: uploadUrl,
      error: resumeResponse.errorMessage,
      // Enhanced tracking fields
      uploadedBytes: resumeResponse.uploadedBytes,
      progressPercentage: resumeResponse.progressPercentage,
      uploadSpeed: 0, // Will be calculated during upload
      estimatedRemainingTime: undefined,
      missingChunks: new Set(resumeResponse.missingChunks),
      nextExpectedChunk: resumeResponse.nextExpectedChunk,
      canResume: resumeResponse.canResume,
      createdAt: new Date(resumeResponse.createdAt),
      lastUpdated: new Date(resumeResponse.lastUpdated)
    };

    this.logger.info('Resuming upload from backend state', {
      fileId: resumeResponse.fileId,
      receivedChunks: resumeResponse.receivedChunks.length,
      missingChunks: resumeResponse.missingChunks.length,
      progressPercentage: resumeResponse.progressPercentage
    });

    const currentUploads = this.uploadsSubject.getValue();
    this.uploadsSubject.next([...currentUploads, resumedUpload]);

    if (resumeResponse.canResume && !resumeResponse.completed && !resumeResponse.failed) {
      this.startUpload(resumedUpload);
    }
  }

  private startUpload(upload: UploadFile): void {
    upload.status = 'uploading';
    this.updateUploadStatus(upload);

    // Use worker for non-blocking UI and better performance
    this.startWorkerUpload(upload);
  }

  private startWorkerUpload(upload: UploadFile): void {
    const message: UploadWorkerMessage = {
      type: 'start',
      payload: {
        fileId: upload.id,
        file: upload.file,
        chunkSize: upload.chunkSize,
        totalChunks: upload.totalChunks,
        uploadUrl: upload.uploadUrl,
        uploadedChunks: Array.from(upload.uploadedChunks)
      }
    };
    this.worker.postMessage(message);
  }

  pauseUpload(fileId: string): void {
    const upload = this.getUploadById(fileId);
    if (upload && upload.status === 'uploading') {
      upload.status = 'paused';
      this.updateUploadStatus(upload);
      const message: UploadWorkerMessage = {
        type: 'pause',
        payload: { fileId }
      };
      this.worker.postMessage(message);
    }
  }

  resumeUpload(fileId: string): void {
    const upload = this.getUploadById(fileId);
    if (upload && (upload.status === 'paused' || upload.status === 'failed')) {
      upload.status = 'uploading';
      this.updateUploadStatus(upload);
      const message: UploadWorkerMessage = {
        type: 'resume',
        payload: {
          fileId: upload.id,
          file: upload.file,
          chunkSize: upload.chunkSize,
          totalChunks: upload.totalChunks,
          uploadUrl: upload.uploadUrl,
          uploadedChunks: Array.from(upload.uploadedChunks)
        }
      };
      this.worker.postMessage(message);
    }
  }

  cancelUpload(fileId: string): void {
    const upload = this.getUploadById(fileId);
    if (upload) {
      upload.status = 'cancelled';
      this.updateUploadStatus(upload);
      this.handleUploadCompletion(fileId, 'cancelled');

      const message: UploadWorkerMessage = {
        type: 'cancel',
        payload: { fileId }
      };
      this.worker.postMessage(message);

      // Also call backend to clean up temporary files
      this.http.delete(`${this.baseUrl}/${fileId}`).subscribe({
        next: () => console.log(`Cleanup completed for fileId: ${fileId}`),
        error: (error) => console.error(`Failed to cleanup fileId: ${fileId}`, error)
      });
    }
  }

  private getUploadById(fileId: string): UploadFile | undefined {
    return this.uploadsSubject.getValue().find(u => u.id === fileId);
  }

  private updateUploadStatus(updatedUpload: UploadFile): void {
    const currentUploads = this.uploadsSubject.getValue();
    const index = currentUploads.findIndex(u => u.id === updatedUpload.id);
    if (index > -1) {
      currentUploads[index] = updatedUpload;
      this.uploadsSubject.next([...currentUploads]);
    }
  }

  getAllUploadStatuses(): Observable<any[]> {
    const statusUrl = this.configService.getStatusUrl();
    this.logger.debug('Fetching upload statuses', { statusUrl });

    return this.http.get<any[]>(statusUrl).pipe(
      catchError(error => {
        this.logger.error('Failed to fetch upload statuses', { error, statusUrl });
        this.errorHandler.handleError(error, {
          component: 'UploadService',
          action: 'getAllUploadStatuses'
        });

        // Return empty array on error to prevent UI breaking
        return of([]);
      })
    );
  }

  getUploadStatus(fileId: string): Observable<BackendUploadStatus> {
    const statusUrl = `${this.configService.getStatusUrl()}/${fileId}`;
    this.logger.debug('Fetching upload status', { fileId, statusUrl });
    return this.http.get<BackendUploadStatus>(statusUrl).pipe(
      catchError(error => {
        this.logger.error('Failed to fetch upload status', { error, fileId, statusUrl });
        this.errorHandler.handleError(error, {
          component: 'UploadService',
          action: 'getUploadStatus'
        });
        throw error;
      })
    );
  }

  /**
   * Resume or initialize an upload session from backend
   */
  resumeUploadFromBackend(fileId: string, totalChunks: number, fileName?: string, fileSize?: number, chunkSize?: number): Observable<ResumeUploadResponse> {
    const resumeUrl = this.configService.getResumeUrl(fileId);
    let params = new HttpParams()
      .set('totalChunks', totalChunks.toString());

    if (fileName) {
      params = params.set('fileName', fileName);
    }
    if (fileSize) {
      params = params.set('fileSize', fileSize.toString());
    }
    if (chunkSize) {
      params = params.set('chunkSize', chunkSize.toString());
    }

    this.logger.debug('Resuming upload', { fileId, totalChunks, fileName, fileSize, chunkSize, resumeUrl });

    return this.http.post<ResumeUploadResponse>(resumeUrl, null, { params }).pipe(
      catchError(error => {
        this.logger.error('Failed to resume upload', { error, fileId, resumeUrl });
        this.errorHandler.handleError(error, {
          component: 'UploadService',
          action: 'resumeUpload'
        });
        throw error;
      })
    );
  }

  /**
   * Get all resumable uploads
   */
  getResumableUploads(): Observable<BackendUploadStatus[]> {
    const resumableUrl = this.configService.getResumableUploadsUrl();
    this.logger.debug('Fetching resumable uploads', { resumableUrl });

    return this.http.get<BackendUploadStatus[]>(resumableUrl).pipe(
      catchError(error => {
        this.logger.error('Failed to fetch resumable uploads', { error, resumableUrl });
        this.errorHandler.handleError(error, {
          component: 'UploadService',
          action: 'getResumableUploads'
        });

        // Return empty array on error to prevent UI breaking
        return of([]);
      })
    );
  }

  /**
   * Check if an upload can be resumed from the backend
   */
  checkResumeCapability(fileId: string): Observable<boolean> {
    return this.getUploadStatus(fileId).pipe(
      catchError(() => of({ canResume: false } as BackendUploadStatus)),
      // Map the response to just the canResume boolean
      map(status => status.canResume),
      catchError(() => of(false))
    );
  }

  /**
   * Sync local upload state with backend state
   */
  syncUploadWithBackend(fileId: string): Observable<UploadFile | null> {
    return this.getUploadStatus(fileId).pipe(
      map(backendStatus => this.convertBackendStatusToUploadFile(backendStatus)),
      catchError(() => of(null))
    );
  }

  /**
   * Convert backend upload status to local UploadFile format
   */
  private convertBackendStatusToUploadFile(backendStatus: BackendUploadStatus, originalFile?: File): UploadFile {
    const status = this.mapBackendStatusToLocalStatus(backendStatus);

    return {
      id: backendStatus.fileId,
      file: originalFile || new File([], backendStatus.fileName || 'unknown'), // Placeholder if no original file
      name: backendStatus.fileName || 'unknown',
      size: backendStatus.fileSize || 0,
      progress: backendStatus.progressPercentage,
      status: status,
      uploadedChunks: new Set(backendStatus.receivedChunks),
      totalChunks: backendStatus.totalChunks,
      chunkSize: backendStatus.chunkSize || this.configService.upload.chunkSize,
      uploadUrl: this.configService.getUploadUrl(),
      error: backendStatus.errorMessage,
      // Enhanced tracking fields
      uploadedBytes: backendStatus.uploadedBytes,
      progressPercentage: backendStatus.progressPercentage,
      uploadSpeed: backendStatus.uploadSpeed,
      estimatedRemainingTime: backendStatus.estimatedRemainingTime,
      missingChunks: new Set(backendStatus.missingChunks),
      nextExpectedChunk: backendStatus.nextExpectedChunk,
      canResume: backendStatus.canResume,
      createdAt: new Date(backendStatus.createdAt),
      lastUpdated: new Date(backendStatus.lastUpdated)
    };
  }

  /**
   * Map backend status to local status
   */
  private mapBackendStatusToLocalStatus(backendStatus: BackendUploadStatus): UploadFile['status'] {
    if (backendStatus.failed) {
      return 'failed';
    }
    if (backendStatus.complete) {
      return 'completed';
    }
    if (backendStatus.canResume && backendStatus.receivedChunks.length > 0) {
      return 'paused'; // Treat resumable uploads as paused
    }
    if (backendStatus.receivedChunks.length > 0) {
      return 'uploading';
    }
    return 'pending';
  }

  /**
   * Determine status for resumed upload
   */
  private determineResumeStatus(resumeResponse: ResumeUploadResponse): UploadFile['status'] {
    if (resumeResponse.completed) {
      return 'completed';
    }
    if (resumeResponse.failed) {
      return 'failed';
    }
    return 'paused';
  }

  /**
   * Initialize resumable uploads on service startup
   */
  private initializeResumableUploads(): void {
    this.logger.info('Initializing resumable uploads');

    this.getResumableUploads().subscribe({
      next: (resumableUploads) => {
        this.logger.info('Found resumable uploads', { count: resumableUploads.length });

        // Convert backend uploads to local format (without File objects)
        const localUploads: UploadFile[] = resumableUploads.map(backendUpload =>
          this.convertBackendStatusToUploadFile(backendUpload)
        );

        if (localUploads.length > 0) {
          this.uploadsSubject.next(localUploads);
          this.notificationService.resumableUploadsFound(localUploads.length);
        }
      },
      error: (error) => {
        this.logger.error('Failed to initialize resumable uploads', { error });
        // Don't show error to user as this is background initialization
      }
    });
  }

  /**
   * Resume upload with original file (for user-initiated resume)
   */
  resumeUploadWithFile(fileId: string, file: File): void {
    this.logger.info('Resuming upload with file', { fileId, fileName: file.name });

    const chunkSize = this.configService.upload.chunkSize;
    const totalChunks = Math.ceil(file.size / chunkSize);

    this.resumeUploadFromBackend(fileId, totalChunks, file.name, file.size, chunkSize)
      .subscribe({
        next: (resumeResponse) => {
          this.logger.info('Resume with file successful', { fileId, resumeResponse });
          this.handleResumeResponse(resumeResponse, file, this.configService.getUploadUrl());
        },
        error: (error) => {
          this.logger.error('Resume with file failed', { fileId, error });
          this.errorHandler.handleError(error, {
            component: 'UploadService',
            action: 'resumeUploadWithFile'
          });
          this.notificationService.resumeUploadFailed(file.name, error.message);
        }
      });
  }

  /**
   * Handle browser refresh scenarios
   */
  private handleBrowserRefresh(): void {
    if (this.browserRefreshService.wasRefreshed) {
      const previousUploads = this.browserRefreshService.getPreviousUploads();
      this.logger.info('Handling browser refresh', { previousUploads: previousUploads.length });

      if (previousUploads.length > 0) {
        this.notificationService.browserRefreshDetected(previousUploads.length);

        // Load resumable uploads from backend
        this.getResumableUploads().subscribe({
          next: (resumableUploads) => {
            const matchingUploads = resumableUploads.filter(upload =>
              previousUploads.includes(upload.fileId)
            );

            if (matchingUploads.length > 0) {
              this.logger.info('Found matching resumable uploads after refresh', {
                count: matchingUploads.length
              });

              // Convert to local format and add to uploads
              const localUploads = matchingUploads.map(upload =>
                this.convertBackendStatusToUploadFile(upload)
              );

              this.uploadsSubject.next(localUploads);
              this.notificationService.uploadsRestoredAfterRefresh(localUploads.length);
            }
          },
          error: (error) => {
            this.logger.error('Failed to restore uploads after refresh', { error });
            this.notificationService.uploadRestoreAfterRefreshFailed();
          }
        });
      }

      // Clear refresh state after handling
      this.browserRefreshService.clearRefreshState();
    }
  }

  /**
   * Update upload completion tracking
   */
  private handleUploadCompletion(fileId: string, status: 'completed' | 'failed' | 'cancelled'): void {
    this.browserRefreshService.unregisterUpload(fileId);
    this.logger.debug('Upload completion handled', { fileId, status });
  }
}
