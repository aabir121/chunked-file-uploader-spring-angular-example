import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UploadFile } from '../../../models/upload.model';
import { UploadUtils } from '../../utils/upload.utils';

@Component({
  selector: 'app-upload-actions',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="upload-actions">
      <button 
        *ngIf="canPause"
        class="action-btn pause-btn"
        (click)="onPause()"
        [disabled]="disabled"
        title="Pause upload">
        ⏸️ Pause
      </button>
      
      <button 
        *ngIf="canResume"
        class="action-btn resume-btn"
        (click)="onResume()"
        [disabled]="disabled"
        title="Resume upload">
        ▶️ Resume
      </button>
      
      <button 
        *ngIf="canCancel"
        class="action-btn cancel-btn"
        (click)="onCancel()"
        [disabled]="disabled"
        title="Cancel upload">
        ❌ Cancel
      </button>
      
      <button 
        *ngIf="canRetry"
        class="action-btn retry-btn"
        (click)="onRetry()"
        [disabled]="disabled"
        title="Retry upload">
        🔄 Retry
      </button>
      
      <button 
        *ngIf="canRemove"
        class="action-btn remove-btn"
        (click)="onRemove()"
        [disabled]="disabled"
        title="Remove from list">
        🗑️ Remove
      </button>
      
      <button 
        *ngIf="canDownload"
        class="action-btn download-btn"
        (click)="onDownload()"
        [disabled]="disabled"
        title="Download file">
        ⬇️ Download
      </button>
    </div>
  `,
  styles: [`
    .upload-actions {
      display: flex;
      gap: var(--spacing-sm);
      flex-wrap: wrap;
      align-items: center;
    }

    .action-btn {
      padding: var(--spacing-sm) var(--spacing-md);
      border: 1px solid var(--secondary-300);
      border-radius: var(--radius-md);
      background: rgba(255, 255, 255, 0.9);
      backdrop-filter: blur(10px);
      cursor: pointer;
      font-size: var(--text-xs);
      font-weight: var(--font-medium);
      transition: all var(--transition-fast);
      white-space: nowrap;
      position: relative;
      overflow: hidden;
      box-shadow: var(--shadow-sm);
    }

    .action-btn::before {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
      transition: left 0.5s ease;
    }

    .action-btn:hover:not(:disabled) {
      transform: translateY(-2px) scale(1.05);
      box-shadow: var(--shadow-md);
    }

    .action-btn:hover:not(:disabled)::before {
      left: 100%;
    }

    .action-btn:active:not(:disabled) {
      transform: translateY(0) scale(1.02);
      box-shadow: var(--shadow-sm);
    }

    .action-btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
      transform: none;
    }

    .pause-btn:hover:not(:disabled) {
      background: linear-gradient(135deg, var(--warning-100), var(--warning-200));
      border-color: var(--warning-400);
      color: var(--warning-700);
    }

    .resume-btn:hover:not(:disabled) {
      background: linear-gradient(135deg, var(--success-100), var(--success-200));
      border-color: var(--success-400);
      color: var(--success-700);
    }

    .cancel-btn:hover:not(:disabled) {
      background: linear-gradient(135deg, var(--error-100), var(--error-200));
      border-color: var(--error-400);
      color: var(--error-700);
    }

    .retry-btn:hover:not(:disabled) {
      background: linear-gradient(135deg, var(--primary-100), var(--primary-200));
      border-color: var(--primary-400);
      color: var(--primary-700);
    }

    .remove-btn:hover:not(:disabled) {
      background: linear-gradient(135deg, var(--error-100), var(--error-200));
      border-color: var(--error-400);
      color: var(--error-700);
    }

    .download-btn:hover:not(:disabled) {
      background: linear-gradient(135deg, var(--success-100), var(--success-200));
      border-color: var(--success-400);
      color: var(--success-700);
    }
  `]
})
export class UploadActionsComponent {
  @Input() upload!: UploadFile;
  @Input() disabled: boolean = false;
  @Input() showPause: boolean = true;
  @Input() showResume: boolean = true;
  @Input() showCancel: boolean = true;
  @Input() showRetry: boolean = true;
  @Input() showRemove: boolean = true;
  @Input() showDownload: boolean = false;

  @Output() pause = new EventEmitter<string>();
  @Output() resume = new EventEmitter<string>();
  @Output() cancel = new EventEmitter<string>();
  @Output() retry = new EventEmitter<string>();
  @Output() remove = new EventEmitter<string>();
  @Output() download = new EventEmitter<string>();

  get canPause(): boolean {
    return this.showPause && 
           this.upload.status === 'uploading';
  }

  get canResume(): boolean {
    return this.showResume && 
           UploadUtils.canResumeUpload(this.upload);
  }

  get canCancel(): boolean {
    return this.showCancel && 
           (this.upload.status === 'uploading' || 
            this.upload.status === 'paused' || 
            this.upload.status === 'pending');
  }

  get canRetry(): boolean {
    return this.showRetry && 
           UploadUtils.isUploadFailed(this.upload);
  }

  get canRemove(): boolean {
    return this.showRemove && 
           (this.upload.status === 'completed' || 
            this.upload.status === 'failed' || 
            this.upload.status === 'cancelled');
  }

  get canDownload(): boolean {
    return this.showDownload && 
           UploadUtils.isUploadCompleted(this.upload);
  }

  onPause(): void {
    this.pause.emit(this.upload.id);
  }

  onResume(): void {
    this.resume.emit(this.upload.id);
  }

  onCancel(): void {
    this.cancel.emit(this.upload.id);
  }

  onRetry(): void {
    this.retry.emit(this.upload.id);
  }

  onRemove(): void {
    this.remove.emit(this.upload.id);
  }

  onDownload(): void {
    this.download.emit(this.upload.id);
  }
}
