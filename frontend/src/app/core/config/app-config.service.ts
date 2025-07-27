import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

export interface ApiConfig {
  baseUrl: string;
  uploadEndpoint: string;
  binaryUploadEndpoint: string;
  statusEndpoint: string;
  resumeEndpoint: string;
  resumableUploadsEndpoint: string;
}

export interface UploadConfig {
  chunkSize: number;
  maxConcurrentUploads: number;
  retryAttempts: number;
  retryDelay: number;
  timeoutMs: number;
}

export interface UiConfig {
  progressUpdateInterval: number;
  autoCloseModalDelay: number;
  maxFileSize: number;
  allowedFileTypes: string[];
}

export interface LoggingConfig {
  level: 'error' | 'warn' | 'info' | 'debug';
  enableConsoleLogging: boolean;
  enableRemoteLogging: boolean;
}

export interface AppConfig {
  production: boolean;
  api: ApiConfig;
  upload: UploadConfig;
  ui: UiConfig;
  logging: LoggingConfig;
}

@Injectable({
  providedIn: 'root'
})
export class AppConfigService {
  private readonly config: AppConfig = environment;

  get production(): boolean {
    return this.config.production;
  }

  get api(): ApiConfig {
    return this.config.api;
  }

  get upload(): UploadConfig {
    return this.config.upload;
  }

  get ui(): UiConfig {
    return this.config.ui;
  }

  get logging(): LoggingConfig {
    return this.config.logging;
  }

  /**
   * Get the full upload URL
   */
  getUploadUrl(): string {
    return `${this.api.baseUrl}${this.api.uploadEndpoint}`;
  }

  /**
   * Get the full binary upload URL
   */
  getBinaryUploadUrl(): string {
    return `${this.api.baseUrl}${this.api.binaryUploadEndpoint}`;
  }

  /**
   * Get the full status URL
   */
  getStatusUrl(): string {
    return `${this.api.baseUrl}${this.api.statusEndpoint}`;
  }

  /**
   * Get the resume upload URL for a specific file
   */
  getResumeUrl(fileId: string): string {
    return `${this.api.baseUrl}${this.api.uploadEndpoint}/${fileId}/resume`;
  }

  /**
   * Get the resumable uploads URL
   */
  getResumableUploadsUrl(): string {
    return `${this.api.baseUrl}${this.api.uploadEndpoint}/resumable`;
  }

  /**
   * Check if a file type is allowed
   */
  isFileTypeAllowed(fileType: string): boolean {
    if (this.ui.allowedFileTypes.includes('*')) {
      return true;
    }
    return this.ui.allowedFileTypes.includes(fileType);
  }

  /**
   * Check if file size is within limits
   */
  isFileSizeAllowed(fileSize: number): boolean {
    return fileSize <= this.ui.maxFileSize;
  }

  /**
   * Validate file before upload
   */
  validateFile(file: File): { valid: boolean; error?: string } {
    if (!this.isFileSizeAllowed(file.size)) {
      return {
        valid: false,
        error: `File size exceeds maximum allowed size of ${this.formatFileSize(this.ui.maxFileSize)}`
      };
    }

    if (!this.isFileTypeAllowed(file.type)) {
      return {
        valid: false,
        error: `File type '${file.type}' is not allowed`
      };
    }

    return { valid: true };
  }

  /**
   * Format file size for display
   */
  formatFileSize(bytes: number): string {
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    if (bytes === 0) return '0 Bytes';
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i];
  }
}
