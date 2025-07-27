import { Injectable, ErrorHandler } from '@angular/core';
import { LoggerService } from '../logging/logger.service';
import { AppConfigService } from '../config/app-config.service';

export interface AppError {
  id: string;
  message: string;
  code?: string;
  details?: any;
  timestamp: Date;
  source?: string;
  severity: 'low' | 'medium' | 'high' | 'critical';
  userMessage?: string;
  recoverable: boolean;
}

export interface ErrorContext {
  component?: string;
  action?: string;
  userId?: string;
  sessionId?: string;
  additionalData?: any;
}

@Injectable({
  providedIn: 'root'
})
export class AppErrorHandler implements ErrorHandler {
  private errors: AppError[] = [];
  private readonly maxStoredErrors = 100;

  constructor(
    private loggerService: LoggerService,
    private configService: AppConfigService
  ) {}

  handleError(error: any, context?: ErrorContext): void {
    const appError = this.createAppError(error, context);
    this.storeError(appError);
    this.logError(appError);
    
    if (this.shouldNotifyUser(appError)) {
      this.notifyUser(appError);
    }
  }

  private createAppError(error: any, context?: ErrorContext): AppError {
    const timestamp = new Date();
    const id = this.generateErrorId();
    
    let message = 'An unexpected error occurred';
    let code: string | undefined;
    let details: any;
    let severity: AppError['severity'] = 'medium';
    let userMessage: string | undefined;
    let recoverable = true;

    // Handle different error types
    if (error instanceof Error) {
      message = error.message;
      details = {
        name: error.name,
        stack: error.stack
      };
    } else if (typeof error === 'string') {
      message = error;
    } else if (error?.error) {
      // HTTP error response
      message = error.error.message || error.message || 'HTTP Error';
      code = error.status?.toString();
      details = {
        status: error.status,
        statusText: error.statusText,
        url: error.url,
        headers: error.headers
      };
      severity = this.getHttpErrorSeverity(error.status);
      userMessage = this.getHttpErrorUserMessage(error.status);
      recoverable = this.isHttpErrorRecoverable(error.status);
    } else {
      details = error;
    }

    return {
      id,
      message,
      code,
      details,
      timestamp,
      source: context?.component,
      severity,
      userMessage,
      recoverable
    };
  }

  private generateErrorId(): string {
    return `err_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  private getHttpErrorSeverity(status: number): AppError['severity'] {
    if (status >= 500) return 'high';
    if (status >= 400) return 'medium';
    return 'low';
  }

  private getHttpErrorUserMessage(status: number): string {
    const messages: Record<number, string> = {
      400: 'Invalid request. Please check your input and try again.',
      401: 'You are not authorized to perform this action.',
      403: 'Access denied. You do not have permission to access this resource.',
      404: 'The requested resource was not found.',
      408: 'Request timeout. Please try again.',
      413: 'File is too large. Please select a smaller file.',
      415: 'File type is not supported.',
      429: 'Too many requests. Please wait a moment and try again.',
      500: 'Server error. Please try again later.',
      502: 'Service temporarily unavailable. Please try again later.',
      503: 'Service temporarily unavailable. Please try again later.',
      504: 'Request timeout. Please try again later.'
    };

    return messages[status] || 'An error occurred. Please try again.';
  }

  private isHttpErrorRecoverable(status: number): boolean {
    // 4xx client errors are generally not recoverable by retry
    // 5xx server errors might be recoverable
    return status >= 500 || status === 408 || status === 429;
  }

  private storeError(error: AppError): void {
    this.errors.unshift(error);
    
    // Keep only the most recent errors
    if (this.errors.length > this.maxStoredErrors) {
      this.errors = this.errors.slice(0, this.maxStoredErrors);
    }
  }

  private logError(error: AppError): void {
    const logger = this.loggerService.createLogger('ErrorHandler');
    
    switch (error.severity) {
      case 'critical':
        logger.error(`CRITICAL ERROR [${error.id}]: ${error.message}`, error.details);
        break;
      case 'high':
        logger.error(`HIGH SEVERITY [${error.id}]: ${error.message}`, error.details);
        break;
      case 'medium':
        logger.warn(`MEDIUM SEVERITY [${error.id}]: ${error.message}`, error.details);
        break;
      case 'low':
        logger.info(`LOW SEVERITY [${error.id}]: ${error.message}`, error.details);
        break;
    }
  }

  private shouldNotifyUser(error: AppError): boolean {
    // Don't notify for low severity errors or if user message is not set
    return error.severity !== 'low' && !!error.userMessage;
  }

  private notifyUser(error: AppError): void {
    // TODO: Implement user notification (toast, modal, etc.)
    console.warn('User notification:', error.userMessage);
  }

  /**
   * Get all stored errors
   */
  getErrors(): AppError[] {
    return [...this.errors];
  }

  /**
   * Get errors by severity
   */
  getErrorsBySeverity(severity: AppError['severity']): AppError[] {
    return this.errors.filter(error => error.severity === severity);
  }

  /**
   * Clear all stored errors
   */
  clearErrors(): void {
    this.errors = [];
  }

  /**
   * Clear errors older than specified time
   */
  clearOldErrors(maxAgeMs: number = 24 * 60 * 60 * 1000): void {
    const cutoffTime = new Date(Date.now() - maxAgeMs);
    this.errors = this.errors.filter(error => error.timestamp > cutoffTime);
  }

  /**
   * Get error statistics
   */
  getErrorStats(): { total: number; bySeverity: Record<string, number> } {
    const bySeverity: Record<string, number> = {
      low: 0,
      medium: 0,
      high: 0,
      critical: 0
    };

    this.errors.forEach(error => {
      bySeverity[error.severity]++;
    });

    return {
      total: this.errors.length,
      bySeverity
    };
  }
}
