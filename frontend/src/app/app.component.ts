import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { UploadButtonComponent } from './upload-button/upload-button.component';
import { UploadModalComponent } from './upload-modal/upload-modal.component';
import { UploadsTableComponent } from './uploads-table/uploads-table.component';
import { NotificationsComponent } from './shared/components/notifications/notifications.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, UploadButtonComponent, UploadModalComponent, UploadsTableComponent, NotificationsComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'frontend';
  isUploadModalOpen = false;

  openUploadModal(): void {
    this.isUploadModalOpen = true;
  }

  closeUploadModal(): void {
    this.isUploadModalOpen = false;
  }
}
