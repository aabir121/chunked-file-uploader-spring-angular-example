import { Observable } from 'rxjs';
import { UploadChunk, UploadFile } from '../../models/upload.model';

/**
 * Interface for upload strategy implementations
 */
export interface IUploadStrategy {
  uploadChunk(chunk: UploadChunk, uploadUrl: string, signal?: AbortSignal): Observable<any>;
  getName(): string;
  getDescription(): string;
}

/**
 * Interface for upload progress tracking
 */
export interface IUploadProgressTracker {
  updateProgress(fileId: string, progress: number): void;
  updateStatus(fileId: string, status: UploadFile['status']): void;
  updateChunkCompleted(fileId: string, chunkIndex: number): void;
  updateError(fileId: string, error: string): void;
}

/**
 * Interface for upload retry logic
 */
export interface IUploadRetryHandler {
  shouldRetry(error: any, attemptCount: number): boolean;
  getRetryDelay(attemptCount: number): number;
  getMaxRetries(): number;
}

/**
 * Interface for upload validation
 */
export interface IUploadValidator {
  validateFile(file: File): { valid: boolean; error?: string };
  validateChunk(chunk: UploadChunk): { valid: boolean; error?: string };
}

/**
 * Interface for upload worker communication
 */
export interface IUploadWorkerManager {
  startUpload(upload: UploadFile): void;
  pauseUpload(fileId: string): void;
  resumeUpload(fileId: string): void;
  cancelUpload(fileId: string): void;
  terminate(): void;
}

/**
 * Upload configuration interface
 */
export interface IUploadConfig {
  chunkSize: number;
  maxConcurrentUploads: number;
  retryAttempts: number;
  retryDelay: number;
  timeoutMs: number;
}

/**
 * Upload service interface
 */
export interface IUploadService {
  uploads$: Observable<UploadFile[]>;
  uploadFile(file: File, uploadUrl?: string): void;
  pauseUpload(fileId: string): void;
  resumeUpload(fileId: string): void;
  cancelUpload(fileId: string): void;
  getAllUploadStatuses(): Observable<any[]>;
}

/**
 * Upload event types
 */
export enum UploadEventType {
  STARTED = 'started',
  PROGRESS = 'progress',
  CHUNK_COMPLETED = 'chunkCompleted',
  PAUSED = 'paused',
  RESUMED = 'resumed',
  COMPLETED = 'completed',
  FAILED = 'failed',
  CANCELLED = 'cancelled'
}

/**
 * Upload event interface
 */
export interface UploadEvent {
  type: UploadEventType;
  fileId: string;
  data?: any;
  timestamp: Date;
}
