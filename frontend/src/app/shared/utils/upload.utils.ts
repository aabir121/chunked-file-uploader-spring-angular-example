import { UploadFile } from '../../models/upload.model';
import { UPLOAD_CONSTANTS } from '../constants/upload.constants';

/**
 * Upload utility functions
 */
export class UploadUtils {
  /**
   * Calculate upload progress percentage
   */
  static calculateProgress(uploadedChunks: Set<number>, totalChunks: number): number {
    if (totalChunks === 0) return 0;
    return Math.round((uploadedChunks.size / totalChunks) * 100);
  }

  /**
   * Calculate estimated time remaining
   */
  static calculateETA(
    uploadedBytes: number,
    totalBytes: number,
    startTime: number,
    currentTime: number = Date.now()
  ): number {
    if (uploadedBytes === 0 || uploadedBytes >= totalBytes) return 0;
    
    const elapsedTime = currentTime - startTime;
    const uploadRate = uploadedBytes / elapsedTime; // bytes per millisecond
    const remainingBytes = totalBytes - uploadedBytes;
    
    return Math.round(remainingBytes / uploadRate);
  }

  /**
   * Format ETA in human-readable format
   */
  static formatETA(etaMs: number): string {
    if (etaMs <= 0) return 'Complete';
    
    const seconds = Math.floor(etaMs / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    
    if (hours > 0) {
      return `${hours}h ${minutes % 60}m`;
    } else if (minutes > 0) {
      return `${minutes}m ${seconds % 60}s`;
    } else {
      return `${seconds}s`;
    }
  }

  /**
   * Calculate upload speed
   */
  static calculateUploadSpeed(
    uploadedBytes: number,
    elapsedTimeMs: number
  ): number {
    if (elapsedTimeMs === 0) return 0;
    return uploadedBytes / (elapsedTimeMs / 1000); // bytes per second
  }

  /**
   * Format upload speed in human-readable format
   */
  static formatUploadSpeed(bytesPerSecond: number): string {
    const units = ['B/s', 'KB/s', 'MB/s', 'GB/s'];
    let unitIndex = 0;
    let speed = bytesPerSecond;
    
    while (speed >= 1024 && unitIndex < units.length - 1) {
      speed /= 1024;
      unitIndex++;
    }
    
    return `${speed.toFixed(1)} ${units[unitIndex]}`;
  }

  /**
   * Calculate optimal chunk size based on file size and connection speed
   */
  static calculateOptimalChunkSize(
    fileSize: number,
    connectionSpeedBps: number = 0,
    defaultChunkSize: number = UPLOAD_CONSTANTS.DEFAULT_CHUNK_SIZE
  ): number {
    // For small files, use smaller chunks
    if (fileSize < 10 * 1024 * 1024) { // < 10MB
      return Math.min(defaultChunkSize, 1024 * 1024); // 1MB max
    }
    
    // For large files, use larger chunks if connection is fast
    if (connectionSpeedBps > 10 * 1024 * 1024) { // > 10 Mbps
      return Math.min(defaultChunkSize * 2, UPLOAD_CONSTANTS.MAX_CHUNK_SIZE);
    }
    
    return defaultChunkSize;
  }

  /**
   * Check if upload can be resumed
   */
  static canResumeUpload(upload: UploadFile): boolean {
    return upload.status === 'paused' || upload.status === 'failed';
  }

  /**
   * Check if upload is in progress
   */
  static isUploadInProgress(upload: UploadFile): boolean {
    return upload.status === 'uploading' || upload.status === 'completing';
  }

  /**
   * Check if upload is completed
   */
  static isUploadCompleted(upload: UploadFile): boolean {
    return upload.status === 'completed';
  }

  /**
   * Check if upload has failed
   */
  static isUploadFailed(upload: UploadFile): boolean {
    return upload.status === 'failed';
  }

  /**
   * Get remaining chunks to upload
   */
  static getRemainingChunks(upload: UploadFile): number[] {
    const remaining: number[] = [];
    for (let i = 0; i < upload.totalChunks; i++) {
      if (!upload.uploadedChunks.has(i)) {
        remaining.push(i);
      }
    }
    return remaining;
  }

  /**
   * Get uploaded bytes count
   */
  static getUploadedBytes(upload: UploadFile): number {
    const uploadedChunks = upload.uploadedChunks.size;
    const lastChunkSize = upload.size % upload.chunkSize;
    
    if (uploadedChunks === 0) return 0;
    
    // If last chunk is uploaded and it's smaller than regular chunks
    if (upload.uploadedChunks.has(upload.totalChunks - 1) && lastChunkSize > 0) {
      return (uploadedChunks - 1) * upload.chunkSize + lastChunkSize;
    }
    
    return uploadedChunks * upload.chunkSize;
  }

  /**
   * Validate chunk index
   */
  static isValidChunkIndex(chunkIndex: number, totalChunks: number): boolean {
    return chunkIndex >= 0 && chunkIndex < totalChunks;
  }

  /**
   * Generate chunk ranges for parallel uploads
   */
  static generateChunkRanges(
    totalChunks: number,
    maxConcurrent: number
  ): number[][] {
    const ranges: number[][] = [];
    const chunksPerRange = Math.ceil(totalChunks / maxConcurrent);
    
    for (let i = 0; i < maxConcurrent; i++) {
      const start = i * chunksPerRange;
      const end = Math.min(start + chunksPerRange, totalChunks);
      
      if (start < totalChunks) {
        const range: number[] = [];
        for (let j = start; j < end; j++) {
          range.push(j);
        }
        ranges.push(range);
      }
    }
    
    return ranges;
  }

  /**
   * Create a delay promise
   */
  static delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * Retry function with exponential backoff
   */
  static async retryWithBackoff<T>(
    fn: () => Promise<T>,
    maxRetries: number = 3,
    baseDelay: number = 1000
  ): Promise<T> {
    let lastError: Error;
    
    for (let attempt = 0; attempt <= maxRetries; attempt++) {
      try {
        return await fn();
      } catch (error) {
        lastError = error as Error;
        
        if (attempt === maxRetries) {
          throw lastError;
        }
        
        const delay = baseDelay * Math.pow(2, attempt);
        await this.delay(delay);
      }
    }
    
    throw lastError!;
  }

  /**
   * Throttle function calls
   */
  static throttle<T extends (...args: any[]) => any>(
    func: T,
    limit: number
  ): (...args: Parameters<T>) => void {
    let inThrottle: boolean;
    
    return function(this: any, ...args: Parameters<T>) {
      if (!inThrottle) {
        func.apply(this, args);
        inThrottle = true;
        setTimeout(() => inThrottle = false, limit);
      }
    };
  }

  /**
   * Debounce function calls
   */
  static debounce<T extends (...args: any[]) => any>(
    func: T,
    delay: number
  ): (...args: Parameters<T>) => void {
    let timeoutId: any;
    
    return function(this: any, ...args: Parameters<T>) {
      clearTimeout(timeoutId);
      timeoutId = setTimeout(() => func.apply(this, args), delay);
    };
  }
}
