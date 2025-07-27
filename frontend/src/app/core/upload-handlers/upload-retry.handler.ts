import { Injectable } from '@angular/core';
import { IUploadRetryHandler } from '../interfaces/upload.interfaces';
import { AppConfigService } from '../config/app-config.service';
import { LoggerService } from '../logging/logger.service';

@Injectable({
  providedIn: 'root'
})
export class UploadRetryHandler implements IUploadRetryHandler {
  private logger = this.loggerService.createLogger('UploadRetryHandler');

  constructor(
    private configService: AppConfigService,
    private loggerService: LoggerService
  ) {}

  shouldRetry(error: any, attemptCount: number): boolean {
    const maxRetries = this.getMaxRetries();
    
    if (attemptCount >= maxRetries) {
      this.logger.debug(`Max retry attempts (${maxRetries}) reached`, { error, attemptCount });
      return false;
    }

    // Don't retry on certain error types
    if (this.isNonRetryableError(error)) {
      this.logger.debug('Non-retryable error encountered', { error, attemptCount });
      return false;
    }

    // Retry on network errors, timeouts, and 5xx server errors
    if (this.isRetryableError(error)) {
      this.logger.debug(`Retryable error, attempt ${attemptCount + 1}/${maxRetries}`, { error });
      return true;
    }

    return false;
  }

  getRetryDelay(attemptCount: number): number {
    const baseDelay = this.configService.upload.retryDelay;
    // Exponential backoff with jitter
    const exponentialDelay = baseDelay * Math.pow(2, attemptCount);
    const jitter = Math.random() * 0.1 * exponentialDelay; // 10% jitter
    return Math.min(exponentialDelay + jitter, 30000); // Cap at 30 seconds
  }

  getMaxRetries(): number {
    return this.configService.upload.retryAttempts;
  }

  private isRetryableError(error: any): boolean {
    // Network errors
    if (error.name === 'NetworkError' || error.name === 'TimeoutError') {
      return true;
    }

    // HTTP errors
    if (error.status) {
      // Retry on 5xx server errors and some 4xx errors
      return error.status >= 500 || 
             error.status === 408 || // Request Timeout
             error.status === 429 || // Too Many Requests
             error.status === 502 || // Bad Gateway
             error.status === 503 || // Service Unavailable
             error.status === 504;   // Gateway Timeout
    }

    // Connection errors
    if (error.message) {
      const message = error.message.toLowerCase();
      return message.includes('network') ||
             message.includes('timeout') ||
             message.includes('connection') ||
             message.includes('fetch');
    }

    return false;
  }

  private isNonRetryableError(error: any): boolean {
    // Abort errors (user cancelled)
    if (error.name === 'AbortError') {
      return true;
    }

    // Client errors that shouldn't be retried
    if (error.status) {
      return error.status === 400 || // Bad Request
             error.status === 401 || // Unauthorized
             error.status === 403 || // Forbidden
             error.status === 404 || // Not Found
             error.status === 413 || // Payload Too Large
             error.status === 415;   // Unsupported Media Type
    }

    return false;
  }

  /**
   * Create a delay promise for retry logic
   */
  createRetryDelay(attemptCount: number): Promise<void> {
    const delay = this.getRetryDelay(attemptCount);
    this.logger.debug(`Waiting ${delay}ms before retry attempt ${attemptCount + 1}`);
    
    return new Promise(resolve => {
      setTimeout(resolve, delay);
    });
  }
}
