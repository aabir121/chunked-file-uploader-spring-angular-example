import { Component, EventEmitter, Output } from '@angular/core';
import { AppConfigService } from '../core/config/app-config.service';

@Component({
  selector: 'app-upload-button',
  standalone: true,
  imports: [],
  templateUrl: './upload-button.component.html',
  styleUrl: './upload-button.component.scss'
})
export class UploadButtonComponent {
  @Output() uploadClick = new EventEmitter<void>();

  constructor(private readonly configService: AppConfigService) {}

  onClick(): void {
    this.uploadClick.emit();
  }

  get uploadHintText(): string {
    const maxSize = this.configService.formatFileSize(this.configService.ui.maxFileSize);
    const allowedTypes = this.configService.ui.allowedFileTypes;
    const fileTypesText = allowedTypes.includes('*') ? 'All formats accepted' : `Supported: ${allowedTypes.join(', ')}`;
    return `Supports files up to ${maxSize} • ${fileTypesText}`;
  }
}
