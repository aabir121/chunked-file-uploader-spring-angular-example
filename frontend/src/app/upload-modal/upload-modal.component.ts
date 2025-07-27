import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UploadService } from '../upload.service';
import { AppConfigService } from '../core/config/app-config.service';
import { LoggerService } from '../core/logging/logger.service';

@Component({
  selector: 'app-upload-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './upload-modal.component.html',
  styleUrl: './upload-modal.component.scss'
})
export class UploadModalComponent {
  @Input() isOpen: boolean = false;
  @Output() closeModal = new EventEmitter<void>();

  selectedFile: File | null = null;
  isDragOver: boolean = false;
  private readonly logger: any;

  constructor(
    private readonly uploadService: UploadService,
    private readonly configService: AppConfigService,
    private readonly loggerService: LoggerService
  ) {
    this.logger = this.loggerService.createLogger('UploadModalComponent');
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
      this.logger.debug('File selected', {
        fileName: this.selectedFile.name,
        fileSize: this.selectedFile.size,
        fileType: this.selectedFile.type
      });
    }
  }

  onUpload(): void {
    if (this.selectedFile) {
      this.logger.info('Starting upload from modal', { fileName: this.selectedFile.name });

      // Validate file before upload
      const validation = this.configService.validateFile(this.selectedFile);
      if (!validation.valid) {
        this.logger.error('File validation failed', { error: validation.error });
        // TODO: Show error message to user
        return;
      }

      this.uploadService.uploadFile(this.selectedFile);
      this.selectedFile = null; // Clear selected file after initiating upload
      this.closeModal.emit();
    }
  }

  onClose(): void {
    this.selectedFile = null;
    this.closeModal.emit();
  }

  onOverlayClick(event: Event): void {
    // Close modal when clicking on overlay (not on modal content)
    this.onClose();
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.selectedFile = files[0];
      this.logger.debug('File dropped', {
        fileName: this.selectedFile.name,
        fileSize: this.selectedFile.size,
        fileType: this.selectedFile.type
      });
    }
  }

  removeSelectedFile(): void {
    this.selectedFile = null;
    this.logger.debug('Selected file removed');
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  getDisplayFileName(fileName: string): string {
    // If filename is too long, show beginning and end with ellipsis
    const maxLength = 50;
    if (fileName.length <= maxLength) {
      return fileName;
    }

    const extension = fileName.split('.').pop() || '';
    const nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));

    if (nameWithoutExt.length > maxLength - extension.length - 3) {
      const keepStart = Math.floor((maxLength - extension.length - 3) / 2);
      const keepEnd = Math.ceil((maxLength - extension.length - 3) / 2);
      return `${nameWithoutExt.substring(0, keepStart)}...${nameWithoutExt.substring(nameWithoutExt.length - keepEnd)}.${extension}`;
    }

    return fileName;
  }

  get maxFileSizeText(): string {
    return this.configService.formatFileSize(this.configService.ui.maxFileSize);
  }

  get allowedFileTypesText(): string {
    const allowedTypes = this.configService.ui.allowedFileTypes;
    if (allowedTypes.includes('*')) {
      return 'All file types supported';
    }
    return `Supported types: ${allowedTypes.join(', ')}`;
  }
}
