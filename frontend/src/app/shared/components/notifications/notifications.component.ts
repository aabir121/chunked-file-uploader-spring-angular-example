import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable, Subscription } from 'rxjs';
import { NotificationService, Notification } from '../../../core/notifications/notification.service';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="notifications-container">
      <div 
        *ngFor="let notification of notifications$ | async; trackBy: trackByNotificationId"
        class="notification"
        [class]="'notification-' + notification.type"
        [@slideIn]>
        
        <div class="notification-icon">
          {{ getIcon(notification.type) }}
        </div>
        
        <div class="notification-content">
          <div class="notification-title">{{ notification.title }}</div>
          <div class="notification-message">{{ notification.message }}</div>
          
          <div class="notification-actions" *ngIf="notification.actions && notification.actions.length > 0">
            <button 
              *ngFor="let action of notification.actions"
              class="notification-action"
              [class]="'action-' + (action.style || 'secondary')"
              (click)="executeAction(action, notification.id)">
              {{ action.label }}
            </button>
          </div>
        </div>
        
        <button 
          class="notification-close"
          (click)="dismiss(notification.id)"
          title="Dismiss">
          ✕
        </button>
      </div>
    </div>
  `,
  styles: [`
    .notifications-container {
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 1000;
      max-width: 400px;
      pointer-events: none;
    }

    .notification {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 16px;
      margin-bottom: 12px;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      backdrop-filter: blur(10px);
      pointer-events: auto;
      position: relative;
      overflow: hidden;
    }

    .notification::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 4px;
    }

    .notification-success {
      background-color: rgba(76, 175, 80, 0.95);
      color: white;
    }

    .notification-success::before {
      background-color: #2e7d32;
    }

    .notification-error {
      background-color: rgba(244, 67, 54, 0.95);
      color: white;
    }

    .notification-error::before {
      background-color: #c62828;
    }

    .notification-warning {
      background-color: rgba(255, 152, 0, 0.95);
      color: white;
    }

    .notification-warning::before {
      background-color: #ef6c00;
    }

    .notification-info {
      background-color: rgba(33, 150, 243, 0.95);
      color: white;
    }

    .notification-info::before {
      background-color: #1565c0;
    }

    .notification-icon {
      font-size: 20px;
      flex-shrink: 0;
      margin-top: 2px;
    }

    .notification-content {
      flex: 1;
      min-width: 0;
    }

    .notification-title {
      font-weight: 600;
      font-size: 14px;
      margin-bottom: 4px;
    }

    .notification-message {
      font-size: 13px;
      line-height: 1.4;
      opacity: 0.9;
      word-wrap: break-word;
    }

    .notification-actions {
      display: flex;
      gap: 8px;
      margin-top: 12px;
    }

    .notification-action {
      padding: 6px 12px;
      border: none;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .action-primary {
      background-color: rgba(255, 255, 255, 0.2);
      color: white;
      border: 1px solid rgba(255, 255, 255, 0.3);
    }

    .action-primary:hover {
      background-color: rgba(255, 255, 255, 0.3);
    }

    .action-secondary {
      background-color: transparent;
      color: white;
      border: 1px solid rgba(255, 255, 255, 0.3);
    }

    .action-secondary:hover {
      background-color: rgba(255, 255, 255, 0.1);
    }

    .action-danger {
      background-color: rgba(255, 255, 255, 0.1);
      color: #ffcdd2;
      border: 1px solid rgba(255, 205, 210, 0.3);
    }

    .action-danger:hover {
      background-color: rgba(255, 205, 210, 0.2);
    }

    .notification-close {
      background: none;
      border: none;
      color: white;
      font-size: 16px;
      cursor: pointer;
      padding: 4px;
      border-radius: 50%;
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0.7;
      transition: all 0.2s ease;
      flex-shrink: 0;
    }

    .notification-close:hover {
      opacity: 1;
      background-color: rgba(255, 255, 255, 0.2);
    }

    @keyframes slideIn {
      from {
        transform: translateX(100%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }

    .notification {
      animation: slideIn 0.3s ease-out;
    }

    @media (max-width: 480px) {
      .notifications-container {
        left: 20px;
        right: 20px;
        max-width: none;
      }
      
      .notification {
        padding: 12px;
      }
      
      .notification-actions {
        flex-direction: column;
      }
    }
  `]
})
export class NotificationsComponent implements OnInit, OnDestroy {
  notifications$: Observable<Notification[]>;
  private subscription?: Subscription;

  constructor(private notificationService: NotificationService) {
    this.notifications$ = this.notificationService.notifications$;
  }

  ngOnInit(): void {
    // Optional: Auto-clear old notifications periodically
    this.subscription = new Subscription();
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  trackByNotificationId(index: number, notification: Notification): string {
    return notification.id;
  }

  getIcon(type: Notification['type']): string {
    const icons = {
      success: '✅',
      error: '❌',
      warning: '⚠️',
      info: 'ℹ️'
    };
    return icons[type];
  }

  dismiss(id: string): void {
    this.notificationService.dismiss(id);
  }

  executeAction(action: any, notificationId: string): void {
    action.action();
    // Optionally dismiss the notification after action
    this.dismiss(notificationId);
  }
}
