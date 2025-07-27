import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-progress-bar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="progress-container">
      <div class="progress-info" *ngIf="showText">
        <span class="progress-percentage">{{ progress | number:'1.0-0' }}%</span>
        <span class="progress-details" *ngIf="showDetails && uploadedBytes && totalBytes">
          ({{ formatBytes(uploadedBytes) }} / {{ formatBytes(totalBytes) }})
        </span>
        <span class="progress-eta" *ngIf="showEta && eta">
          - {{ eta }}
        </span>
      </div>
      <div class="progress-bar-container" [class.animated]="animated">
        <div
          class="progress-bar"
          [style.width.%]="progress"
          [class.success]="progress === 100"
          [class.error]="error"
          [class.paused]="paused">
        </div>
      </div>
    </div>
  `,
  styles: [`
    .progress-container {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .progress-info {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      color: #555;
      line-height: 1;
    }

    .progress-percentage {
      font-weight: 600;
      color: #333;
    }

    .progress-details {
      color: #666;
      font-size: 12px;
    }

    .progress-eta {
      color: #666;
      font-size: 12px;
      font-style: italic;
    }

    .progress-bar-container {
      position: relative;
      width: 100%;
      height: 8px;
      background-color: #e9ecef;
      border-radius: 4px;
      overflow: hidden;
    }

    .progress-bar {
      height: 100%;
      background: linear-gradient(90deg, #28a745 0%, #20c997 100%);
      transition: width 0.3s ease;
      border-radius: 4px;
    }

    .progress-bar.animated {
      transition: width 0.5s ease-in-out;
    }

    .progress-bar.success {
      background: linear-gradient(90deg, #28a745 0%, #20c997 100%);
    }

    .progress-bar.error {
      background: linear-gradient(90deg, #dc3545 0%, #c82333 100%);
    }

    .progress-bar.paused {
      background: linear-gradient(90deg, #ffc107 0%, #e0a800 100%);
    }
  `]
})
export class ProgressBarComponent {
  @Input() progress: number = 0;
  @Input() showText: boolean = true;
  @Input() showEta: boolean = false;
  @Input() showDetails: boolean = true;
  @Input() eta: string = '';
  @Input() uploadedBytes: number = 0;
  @Input() totalBytes: number = 0;
  @Input() animated: boolean = true;
  @Input() error: boolean = false;
  @Input() paused: boolean = false;

  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }
}
