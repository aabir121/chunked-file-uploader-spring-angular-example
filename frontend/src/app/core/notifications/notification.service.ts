import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { LoggerService } from '../logging/logger.service';

export interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  timestamp: Date;
  duration?: number; // Auto-dismiss after this many milliseconds
  persistent?: boolean; // Don't auto-dismiss
  actions?: NotificationAction[];
}

export interface NotificationAction {
  label: string;
  action: () => void;
  style?: 'primary' | 'secondary' | 'danger';
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly notificationsSubject = new BehaviorSubject<Notification[]>([]);
  public readonly notifications$: Observable<Notification[]> = this.notificationsSubject.asObservable();
  
  private readonly defaultDurations = {
    success: 5000,
    info: 5000,
    warning: 8000,
    error: 10000
  };

  private readonly logger: any;

  constructor(private readonly loggerService: LoggerService) {
    this.logger = this.loggerService.createLogger('NotificationService');
  }

  /**
   * Show a success notification
   */
  success(title: string, message: string, options?: Partial<Notification>): string {
    return this.show({
      type: 'success',
      title,
      message,
      ...options
    });
  }

  /**
   * Show an error notification
   */
  error(title: string, message: string, options?: Partial<Notification>): string {
    return this.show({
      type: 'error',
      title,
      message,
      persistent: true, // Errors are persistent by default
      ...options
    });
  }

  /**
   * Show a warning notification
   */
  warning(title: string, message: string, options?: Partial<Notification>): string {
    return this.show({
      type: 'warning',
      title,
      message,
      ...options
    });
  }

  /**
   * Show an info notification
   */
  info(title: string, message: string, options?: Partial<Notification>): string {
    return this.show({
      type: 'info',
      title,
      message,
      ...options
    });
  }

  /**
   * Show a notification
   */
  show(notification: Partial<Notification>): string {
    const id = this.generateId();
    const timestamp = new Date();
    
    const fullNotification: Notification = {
      id,
      timestamp,
      type: 'info',
      title: '',
      message: '',
      duration: this.defaultDurations[notification.type || 'info'],
      ...notification
    };

    this.logger.debug('Showing notification', {
      id: fullNotification.id,
      type: fullNotification.type,
      title: fullNotification.title
    });

    const currentNotifications = this.notificationsSubject.value;
    this.notificationsSubject.next([...currentNotifications, fullNotification]);

    // Auto-dismiss if not persistent
    if (!fullNotification.persistent && fullNotification.duration) {
      setTimeout(() => {
        this.dismiss(id);
      }, fullNotification.duration);
    }

    return id;
  }

  /**
   * Dismiss a notification by ID
   */
  dismiss(id: string): void {
    this.logger.debug('Dismissing notification', { id });
    
    const currentNotifications = this.notificationsSubject.value;
    const updatedNotifications = currentNotifications.filter(n => n.id !== id);
    this.notificationsSubject.next(updatedNotifications);
  }

  /**
   * Dismiss all notifications
   */
  dismissAll(): void {
    this.logger.debug('Dismissing all notifications');
    this.notificationsSubject.next([]);
  }

  /**
   * Dismiss all notifications of a specific type
   */
  dismissByType(type: Notification['type']): void {
    this.logger.debug('Dismissing notifications by type', { type });
    
    const currentNotifications = this.notificationsSubject.value;
    const updatedNotifications = currentNotifications.filter(n => n.type !== type);
    this.notificationsSubject.next(updatedNotifications);
  }

  /**
   * Update an existing notification
   */
  update(id: string, updates: Partial<Notification>): void {
    this.logger.debug('Updating notification', { id, updates });
    
    const currentNotifications = this.notificationsSubject.value;
    const updatedNotifications = currentNotifications.map(notification => 
      notification.id === id 
        ? { ...notification, ...updates }
        : notification
    );
    this.notificationsSubject.next(updatedNotifications);
  }

  /**
   * Get current notifications
   */
  getNotifications(): Notification[] {
    return this.notificationsSubject.value;
  }

  /**
   * Get notifications by type
   */
  getNotificationsByType(type: Notification['type']): Notification[] {
    return this.notificationsSubject.value.filter(n => n.type === type);
  }

  /**
   * Check if there are any notifications of a specific type
   */
  hasNotificationsOfType(type: Notification['type']): boolean {
    return this.notificationsSubject.value.some(n => n.type === type);
  }

  /**
   * Show upload-specific notifications
   */
  uploadStarted(fileName: string): string {
    return this.info(
      'Upload Started',
      `Started uploading "${fileName}"`,
      { duration: 3000 }
    );
  }

  uploadCompleted(fileName: string): string {
    return this.success(
      'Upload Completed',
      `Successfully uploaded "${fileName}"`,
      { duration: 5000 }
    );
  }

  uploadFailed(fileName: string, error: string): string {
    return this.error(
      'Upload Failed',
      `Failed to upload "${fileName}": ${error}`,
      {
        actions: [
          {
            label: 'Retry',
            action: () => {
              // TODO: Implement retry logic
              console.log('Retry upload:', fileName);
            },
            style: 'primary'
          }
        ]
      }
    );
  }

  uploadPaused(fileName: string): string {
    return this.warning(
      'Upload Paused',
      `Upload of "${fileName}" has been paused`,
      { duration: 3000 }
    );
  }

  uploadResumed(fileName: string): string {
    return this.info(
      'Upload Resumed',
      `Resumed uploading "${fileName}"`,
      { duration: 3000 }
    );
  }

  uploadCancelled(fileName: string): string {
    return this.info(
      'Upload Cancelled',
      `Cancelled upload of "${fileName}"`,
      { duration: 3000 }
    );
  }

  fileValidationError(fileName: string, error: string): string {
    return this.error(
      'File Validation Error',
      `Cannot upload "${fileName}": ${error}`
    );
  }

  networkError(): string {
    return this.error(
      'Network Error',
      'Unable to connect to the server. Please check your internet connection and try again.',
      {
        actions: [
          {
            label: 'Retry',
            action: () => {
              // TODO: Implement retry logic
              console.log('Retry network operation');
            },
            style: 'primary'
          }
        ]
      }
    );
  }

  private generateId(): string {
    return `notification_${Date.now()}_${Math.random().toString(36).substring(2, 11)}`;
  }

  /**
   * Show notification when resumable uploads are found
   */
  resumableUploadsFound(count: number): string {
    return this.info(
      'Resumable Uploads Found',
      `Found ${count} upload${count > 1 ? 's' : ''} that can be resumed.`,
      { duration: 8000 }
    );
  }

  /**
   * Show notification when browser refresh is detected
   */
  browserRefreshDetected(count: number): string {
    return this.info(
      'Browser Refresh Detected',
      `Checking for ${count} upload${count > 1 ? 's' : ''} that can be resumed...`,
      { duration: 5000 }
    );
  }

  /**
   * Show notification when uploads are restored after refresh
   */
  uploadsRestoredAfterRefresh(count: number): string {
    return this.success(
      'Uploads Restored',
      `Successfully restored ${count} upload${count > 1 ? 's' : ''} after browser refresh.`,
      { duration: 6000 }
    );
  }

  /**
   * Show notification when upload restore fails after refresh
   */
  uploadRestoreAfterRefreshFailed(): string {
    return this.warning(
      'Upload Restore Failed',
      'Could not restore uploads after browser refresh. You may need to restart your uploads.',
      { duration: 10000 }
    );
  }

  /**
   * Show notification when resume upload fails
   */
  resumeUploadFailed(fileName: string, error: string): string {
    return this.error(
      'Resume Upload Failed',
      `Failed to resume upload for "${fileName}": ${error}`,
      { duration: 10000 }
    );
  }
}
