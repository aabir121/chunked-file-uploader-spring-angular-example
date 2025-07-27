import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileUtils } from '../../utils/file.utils';

@Component({
  selector: 'app-file-info',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="file-info" [class.compact]="compact">
      <div class="file-icon" [class]="getFileTypeClass()">
        <span class="icon">{{ getFileIcon() }}</span>
      </div>
      <div class="file-details">
        <div class="file-name" [title]="fileName">
          {{ displayName }}
        </div>
        <div class="file-meta" *ngIf="showMeta">
          <span class="file-size">{{ formattedSize }}</span>
          <span class="file-type" *ngIf="showType">{{ fileType }}</span>
          <span class="file-modified" *ngIf="showModified && lastModified">
            {{ formatDate(lastModified) }}
          </span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .file-info {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 8px;
      border-radius: 6px;
      background-color: #f9f9f9;
      border: 1px solid #e0e0e0;
    }

    .file-info.compact {
      padding: 4px 8px;
      gap: 8px;
    }

    .file-icon {
      width: 40px;
      height: 40px;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      color: white;
      flex-shrink: 0;
    }

    .file-info.compact .file-icon {
      width: 24px;
      height: 24px;
      font-size: 12px;
    }

    .file-icon.image { background-color: #4CAF50; }
    .file-icon.video { background-color: #2196F3; }
    .file-icon.audio { background-color: #FF9800; }
    .file-icon.document { background-color: #f44336; }
    .file-icon.archive { background-color: #9C27B0; }
    .file-icon.code { background-color: #607D8B; }
    .file-icon.default { background-color: #757575; }

    .file-details {
      flex: 1;
      min-width: 0;
    }

    .file-name {
      font-weight: 500;
      color: #333;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      margin-bottom: 2px;
    }

    .file-info.compact .file-name {
      font-size: 14px;
    }

    .file-meta {
      display: flex;
      gap: 12px;
      font-size: 12px;
      color: #666;
    }

    .file-info.compact .file-meta {
      font-size: 11px;
      gap: 8px;
    }

    .file-size {
      font-weight: 500;
    }

    .file-type {
      text-transform: uppercase;
      background-color: #e0e0e0;
      padding: 1px 4px;
      border-radius: 3px;
      font-size: 10px;
    }

    .file-modified {
      opacity: 0.8;
    }
  `]
})
export class FileInfoComponent {
  @Input() fileName: string = '';
  @Input() fileSize: number = 0;
  @Input() fileType: string = '';
  @Input() lastModified?: number;
  @Input() maxNameLength: number = 50;
  @Input() compact: boolean = false;
  @Input() showMeta: boolean = true;
  @Input() showType: boolean = true;
  @Input() showModified: boolean = false;

  get displayName(): string {
    if (this.fileName.length <= this.maxNameLength) {
      return this.fileName;
    }

    const extension = FileUtils.getFileExtension(this.fileName);
    const baseName = this.fileName.substring(0, this.fileName.lastIndexOf('.'));

    // Reserve space for extension and ellipsis
    const extensionPart = extension ? '.' + extension : '';
    const reservedLength = extensionPart.length + 3; // 3 for "..."
    const availableLength = this.maxNameLength - reservedLength;

    if (availableLength > 10) {
      // Show beginning and end of filename for better context
      const frontLength = Math.floor(availableLength * 0.6);
      const backLength = availableLength - frontLength;

      if (baseName.length > availableLength) {
        const front = baseName.substring(0, frontLength);
        const back = baseName.substring(baseName.length - backLength);
        return `${front}...${back}${extensionPart}`;
      }
    }

    // Fallback to simple truncation
    if (availableLength > 0) {
      return `${baseName.substring(0, availableLength)}...${extensionPart}`;
    }

    return `${this.fileName.substring(0, this.maxNameLength - 3)}...`;
  }

  get formattedSize(): string {
    return FileUtils.formatFileSize(this.fileSize);
  }

  getFileTypeClass(): string {
    const extension = FileUtils.getFileExtension(this.fileName).toLowerCase();
    
    // Image files
    if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'].includes(extension)) {
      return 'image';
    }
    
    // Video files
    if (['mp4', 'avi', 'mov', 'wmv', 'flv', 'webm', 'mkv'].includes(extension)) {
      return 'video';
    }
    
    // Audio files
    if (['mp3', 'wav', 'ogg', 'aac', 'flac'].includes(extension)) {
      return 'audio';
    }
    
    // Document files
    if (['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt'].includes(extension)) {
      return 'document';
    }
    
    // Archive files
    if (['zip', 'rar', '7z', 'tar', 'gz'].includes(extension)) {
      return 'archive';
    }
    
    // Code files
    if (['js', 'ts', 'html', 'css', 'json', 'xml', 'py', 'java', 'cpp', 'c'].includes(extension)) {
      return 'code';
    }
    
    return 'default';
  }

  getFileIcon(): string {
    const type = this.getFileTypeClass();
    
    const icons: Record<string, string> = {
      image: '🖼️',
      video: '🎬',
      audio: '🎵',
      document: '📄',
      archive: '📦',
      code: '💻',
      default: '📁'
    };
    
    return icons[type] || icons['default'];
  }

  formatDate(timestamp: number): string {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    
    if (diffDays === 0) {
      return 'Today';
    } else if (diffDays === 1) {
      return 'Yesterday';
    } else if (diffDays < 7) {
      return `${diffDays} days ago`;
    } else {
      return date.toLocaleDateString();
    }
  }
}
