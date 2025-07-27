export interface UploadFile {
  id: string;
  file: File;
  name: string;
  size: number;
  progress: number;
  status: 'pending' | 'uploading' | 'paused' | 'completing' | 'completed' | 'failed' | 'cancelled';
  uploadedChunks: Set<number>;
  totalChunks: number;
  chunkSize: number;
  uploadUrl: string;
  error?: string;
  // New fields for enhanced tracking
  uploadedBytes?: number;
  progressPercentage?: number;
  uploadSpeed?: number; // bytes per second
  estimatedRemainingTime?: number; // milliseconds
  missingChunks?: Set<number>;
  nextExpectedChunk?: number;
  canResume?: boolean;
  createdAt?: Date;
  lastUpdated?: Date;
}

export interface UploadChunk {
  fileId: string;
  chunkIndex: number;
  data: Blob;
  totalChunks: number;
  fileName: string;
  fileSize: number;
}

export interface UploadWorkerMessage {
  type: 'start' | 'pause' | 'resume' | 'cancel';
  payload: UploadWorkerMessagePayload;
}

export interface UploadWorkerMessagePayload {
  fileId: string;
  file?: File;
  chunkSize?: number;
  totalChunks?: number;
  uploadUrl?: string;
  uploadedChunks?: number[];
}

export interface UploadWorkerResponse {
  type: 'progress' | 'completing' | 'completed' | 'failed' | 'chunkCompleted';
  payload: UploadWorkerResponsePayload;
}

export interface UploadWorkerResponsePayload {
  fileId: string;
  progress?: number;
  uploadedChunks?: number[];
  chunkIndex?: number;
  error?: string;
  // Enhanced tracking fields
  uploadedBytes?: number;
  uploadSpeed?: number;
  estimatedRemainingTime?: number;
}

// Backend API response interfaces
export interface BackendUploadStatus {
  fileId: string;
  totalChunks: number;
  receivedChunks: number[];
  missingChunks: number[];
  nextExpectedChunk: number;
  fileName?: string;
  fileSize?: number;
  chunkSize?: number;
  uploadedBytes: number;
  progressPercentage: number;
  uploadSpeed?: number;
  estimatedRemainingTime?: number;
  canResume: boolean;
  complete: boolean;
  failed: boolean;
  errorMessage?: string;
  createdAt: string;
  lastUpdated: string;
}

export interface ResumeUploadResponse {
  fileId: string;
  totalChunks: number;
  receivedChunks: number[];
  missingChunks: number[];
  nextExpectedChunk: number;
  fileName?: string;
  fileSize?: number;
  chunkSize?: number;
  uploadedBytes: number;
  progressPercentage: number;
  canResume: boolean;
  completed: boolean;
  failed: boolean;
  errorMessage?: string;
  createdAt: string;
  lastUpdated: string;
}

export interface UploadStatistics {
  totalUploads: number;
  completedUploads: number;
  failedUploads: number;
  activeUploads: number;
}
