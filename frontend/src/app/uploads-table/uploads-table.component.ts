import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UploadService } from '../upload.service';
import { UploadFile, BackendUploadStatus } from '../models/upload.model';
import { Observable } from 'rxjs';
import { ProgressBarComponent } from '../shared/components/progress-bar/progress-bar.component';
import { FileInfoComponent } from '../shared/components/file-info/file-info.component';
import { UploadActionsComponent } from '../shared/components/upload-actions/upload-actions.component';
import { UploadUtils } from '../shared/utils/upload.utils';
import { LoggerService } from '../core/logging/logger.service';

@Component({
  selector: 'app-uploads-table',
  standalone: true,
  imports: [CommonModule, ProgressBarComponent, FileInfoComponent, UploadActionsComponent],
  templateUrl: './uploads-table.component.html',
  styleUrl: './uploads-table.component.scss'
})
export class UploadsTableComponent implements OnInit {
  uploads$: Observable<UploadFile[]>;
  backendUploads$!: Observable<BackendUploadStatus[]>; // To store data from backend API
  private readonly logger: any;

  constructor(
    private readonly uploadService: UploadService,
    private readonly loggerService: LoggerService
  ) {
    this.logger = this.loggerService.createLogger('UploadsTableComponent');
    this.uploads$ = this.uploadService.uploads$;
  }

  ngOnInit(): void {
    this.uploads$ = this.uploadService.uploads$;
    this.backendUploads$ = this.uploadService.getAllUploadStatuses();
    this.logger.info('UploadsTableComponent initialized');

    // Refresh backend data periodically
    setInterval(() => {
      this.backendUploads$ = this.uploadService.getAllUploadStatuses();
    }, 5000); // Refresh every 5 seconds
  }

  trackByUploadId(index: number, upload: UploadFile): string {
    return upload.id;
  }

  getStatusText(status: string): string {
    const statusMap: Record<string, string> = {
      'pending': 'Pending',
      'uploading': 'Uploading',
      'paused': 'Paused',
      'completing': 'Finalizing',
      'completed': 'Completed',
      'failed': 'Failed',
      'cancelled': 'Cancelled'
    };
    return statusMap[status] || status;
  }

  getETA(upload: UploadFile): string {
    if (upload.status !== 'uploading' || upload.progress === 0) {
      return '';
    }

    // Use enhanced ETA if available
    if (upload.estimatedRemainingTime) {
      return this.formatTime(upload.estimatedRemainingTime);
    }

    // Fallback to simple ETA calculation based on progress
    const progressRate = upload.progress / 100; // percentage as decimal
    if (progressRate === 0) return '';

    const estimatedTotalTime = Date.now() / progressRate;
    const remainingTime = estimatedTotalTime - Date.now();

    return UploadUtils.formatETA(remainingTime);
  }

  getUploadedBytes(upload: UploadFile): number {
    return UploadUtils.getUploadedBytes(upload);
  }

  getUploadSpeed(upload: UploadFile): string {
    if (upload.uploadSpeed && upload.uploadSpeed > 0) {
      return this.formatBytes(upload.uploadSpeed) + '/s';
    }
    return '';
  }

  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  formatTime(milliseconds: number): string {
    if (!milliseconds || milliseconds <= 0) return '';

    const seconds = Math.floor(milliseconds / 1000);
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

  getBackendStatusText(upload: BackendUploadStatus): string {
    if (upload.failed) {
      return 'Failed';
    }
    if (upload.complete) {
      return 'Completed';
    }
    if (upload.canResume && upload.receivedChunks.length > 0) {
      return 'Resumable';
    }
    if (upload.receivedChunks.length > 0) {
      return 'Processing';
    }
    return 'Pending';
  }

  pauseUpload(fileId: string): void {
    this.logger.debug('Pausing upload', { fileId });
    this.uploadService.pauseUpload(fileId);
  }

  resumeUpload(fileId: string): void {
    this.logger.debug('Resuming upload', { fileId });
    this.uploadService.resumeUpload(fileId);
  }

  cancelUpload(fileId: string): void {
    this.logger.debug('Cancelling upload', { fileId });
    this.uploadService.cancelUpload(fileId);
  }

  retryUpload(fileId: string): void {
    this.logger.debug('Retrying upload', { fileId });
    this.resumeUpload(fileId); // For now, retry is same as resume
  }

  removeUpload(fileId: string): void {
    this.logger.debug('Removing upload from list', { fileId });
    // TODO: Implement remove functionality in upload service
  }

  formatTimestamp(timestamp: string): string {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);

    if (diffMins < 1) {
      return 'Just now';
    } else if (diffMins < 60) {
      return `${diffMins}m ago`;
    } else if (diffMins < 1440) {
      const hours = Math.floor(diffMins / 60);
      return `${hours}h ago`;
    } else {
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
  }

  refreshBackendData(): void {
    this.logger.info('Refreshing backend upload data');
    this.backendUploads$ = this.uploadService.getAllUploadStatuses();
  }
}
