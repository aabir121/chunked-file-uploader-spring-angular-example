import { Injectable } from '@angular/core';
import { AppConfigService } from '../config/app-config.service';

export enum LogLevel {
  ERROR = 0,
  WARN = 1,
  INFO = 2,
  DEBUG = 3
}

export interface LogEntry {
  timestamp: Date;
  level: LogLevel;
  message: string;
  data?: any;
  source?: string;
}

@Injectable({
  providedIn: 'root'
})
export class LoggerService {
  private readonly logLevelMap: Record<string, LogLevel> = {
    'error': LogLevel.ERROR,
    'warn': LogLevel.WARN,
    'info': LogLevel.INFO,
    'debug': LogLevel.DEBUG
  };

  private currentLogLevel: LogLevel;

  constructor(private configService: AppConfigService) {
    this.currentLogLevel = this.logLevelMap[this.configService.logging.level] || LogLevel.INFO;
  }

  error(message: string, data?: any, source?: string): void {
    this.log(LogLevel.ERROR, message, data, source);
  }

  warn(message: string, data?: any, source?: string): void {
    this.log(LogLevel.WARN, message, data, source);
  }

  info(message: string, data?: any, source?: string): void {
    this.log(LogLevel.INFO, message, data, source);
  }

  debug(message: string, data?: any, source?: string): void {
    this.log(LogLevel.DEBUG, message, data, source);
  }

  private log(level: LogLevel, message: string, data?: any, source?: string): void {
    if (level > this.currentLogLevel) {
      return;
    }

    const logEntry: LogEntry = {
      timestamp: new Date(),
      level,
      message,
      data,
      source
    };

    if (this.configService.logging.enableConsoleLogging) {
      this.logToConsole(logEntry);
    }

    if (this.configService.logging.enableRemoteLogging) {
      this.logToRemote(logEntry);
    }
  }

  private logToConsole(entry: LogEntry): void {
    const timestamp = entry.timestamp.toISOString();
    const source = entry.source ? `[${entry.source}]` : '';
    const message = `${timestamp} ${source} ${entry.message}`;

    switch (entry.level) {
      case LogLevel.ERROR:
        console.error(message, entry.data);
        break;
      case LogLevel.WARN:
        console.warn(message, entry.data);
        break;
      case LogLevel.INFO:
        console.info(message, entry.data);
        break;
      case LogLevel.DEBUG:
        console.debug(message, entry.data);
        break;
    }
  }

  private logToRemote(entry: LogEntry): void {
    // TODO: Implement remote logging if needed
    // This could send logs to a remote logging service
    console.debug('Remote logging not implemented yet', entry);
  }

  /**
   * Create a logger instance for a specific source
   */
  createLogger(source: string) {
    return {
      error: (message: string, data?: any) => this.error(message, data, source),
      warn: (message: string, data?: any) => this.warn(message, data, source),
      info: (message: string, data?: any) => this.info(message, data, source),
      debug: (message: string, data?: any) => this.debug(message, data, source)
    };
  }
}
