import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { LoggerService } from '../core/logging/logger.service';

export interface BrowserRefreshState {
  wasRefreshed: boolean;
  previousUploads: string[]; // Array of file IDs
  timestamp: number;
}

@Injectable({
  providedIn: 'root'
})
export class BrowserRefreshService {
  private readonly STORAGE_KEY = 'large-upload-state';
  private readonly refreshStateSubject = new BehaviorSubject<BrowserRefreshState>({
    wasRefreshed: false,
    previousUploads: [],
    timestamp: 0
  });
  
  private readonly logger: any;

  constructor(private readonly loggerService: LoggerService) {
    this.logger = this.loggerService.createLogger('BrowserRefreshService');
    this.initializeRefreshDetection();
  }

  get refreshState$(): Observable<BrowserRefreshState> {
    return this.refreshStateSubject.asObservable();
  }

  get wasRefreshed(): boolean {
    return this.refreshStateSubject.getValue().wasRefreshed;
  }

  /**
   * Initialize browser refresh detection
   */
  private initializeRefreshDetection(): void {
    // Check if there's a previous state in sessionStorage
    const savedState = this.loadState();
    
    if (savedState && this.isRecentState(savedState)) {
      this.logger.info('Browser refresh detected', { 
        previousUploads: savedState.previousUploads.length,
        timestamp: savedState.timestamp 
      });
      
      this.refreshStateSubject.next({
        wasRefreshed: true,
        previousUploads: savedState.previousUploads,
        timestamp: savedState.timestamp
      });
    } else {
      this.logger.info('Fresh browser session detected');
      this.clearState();
    }

    // Set up beforeunload listener to save state
    window.addEventListener('beforeunload', () => {
      this.saveCurrentState();
    });

    // Set up visibility change listener for mobile browsers
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'hidden') {
        this.saveCurrentState();
      }
    });
  }

  /**
   * Register an active upload
   */
  registerActiveUpload(fileId: string): void {
    const currentState = this.refreshStateSubject.getValue();
    const updatedUploads = [...currentState.previousUploads];
    
    if (!updatedUploads.includes(fileId)) {
      updatedUploads.push(fileId);
      this.refreshStateSubject.next({
        ...currentState,
        previousUploads: updatedUploads
      });
    }
    
    this.logger.debug('Registered active upload', { fileId, totalActive: updatedUploads.length });
  }

  /**
   * Unregister a completed/cancelled upload
   */
  unregisterUpload(fileId: string): void {
    const currentState = this.refreshStateSubject.getValue();
    const updatedUploads = currentState.previousUploads.filter(id => id !== fileId);
    
    this.refreshStateSubject.next({
      ...currentState,
      previousUploads: updatedUploads
    });
    
    this.logger.debug('Unregistered upload', { fileId, remainingActive: updatedUploads.length });
  }

  /**
   * Clear refresh state (call after handling refresh)
   */
  clearRefreshState(): void {
    this.refreshStateSubject.next({
      wasRefreshed: false,
      previousUploads: [],
      timestamp: 0
    });
    this.clearState();
    this.logger.info('Refresh state cleared');
  }

  /**
   * Save current state to sessionStorage
   */
  private saveCurrentState(): void {
    const currentState = this.refreshStateSubject.getValue();
    
    if (currentState.previousUploads.length > 0) {
      const stateToSave = {
        previousUploads: currentState.previousUploads,
        timestamp: Date.now()
      };
      
      try {
        sessionStorage.setItem(this.STORAGE_KEY, JSON.stringify(stateToSave));
        this.logger.debug('Saved state before unload', { uploads: stateToSave.previousUploads.length });
      } catch (error) {
        this.logger.error('Failed to save state', { error });
      }
    }
  }

  /**
   * Load state from sessionStorage
   */
  private loadState(): { previousUploads: string[]; timestamp: number } | null {
    try {
      const savedData = sessionStorage.getItem(this.STORAGE_KEY);
      if (savedData) {
        return JSON.parse(savedData);
      }
    } catch (error) {
      this.logger.error('Failed to load saved state', { error });
    }
    return null;
  }

  /**
   * Clear state from sessionStorage
   */
  private clearState(): void {
    try {
      sessionStorage.removeItem(this.STORAGE_KEY);
    } catch (error) {
      this.logger.error('Failed to clear state', { error });
    }
  }

  /**
   * Check if the saved state is recent (within last 5 minutes)
   */
  private isRecentState(state: { timestamp: number }): boolean {
    const FIVE_MINUTES = 5 * 60 * 1000;
    return (Date.now() - state.timestamp) < FIVE_MINUTES;
  }

  /**
   * Get the list of uploads that were active before refresh
   */
  getPreviousUploads(): string[] {
    return this.refreshStateSubject.getValue().previousUploads;
  }

  /**
   * Check if a specific upload was active before refresh
   */
  wasUploadActive(fileId: string): boolean {
    return this.refreshStateSubject.getValue().previousUploads.includes(fileId);
  }
}
