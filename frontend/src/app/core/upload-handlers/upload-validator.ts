import { Injectable } from '@angular/core';
import { IUploadValidator } from '../interfaces/upload.interfaces';
import { UploadChunk } from '../../models/upload.model';
import { AppConfigService } from '../config/app-config.service';
import { LoggerService } from '../logging/logger.service';

@Injectable({
  providedIn: 'root'
})
export class UploadValidator implements IUploadValidator {
  private readonly logger: any;

  constructor(
    private readonly configService: AppConfigService,
    private readonly loggerService: LoggerService
  ) {
    this.logger = this.loggerService.createLogger('UploadValidator');
  }

  validateFile(file: File): { valid: boolean; error?: string } {
    this.logger.debug('Validating file', {
      name: file.name,
      size: file.size,
      type: file.type,
      lastModified: file.lastModified
    });

    // Check if file exists
    if (!file) {
      return { valid: false, error: 'No file provided' };
    }

    // Check file size
    if (!this.configService.isFileSizeAllowed(file.size)) {
      const maxSize = this.configService.formatFileSize(this.configService.ui.maxFileSize);
      return {
        valid: false,
        error: `File size (${this.configService.formatFileSize(file.size)}) exceeds maximum allowed size of ${maxSize}`
      };
    }

    // Check file type
    if (!this.configService.isFileTypeAllowed(file.type)) {
      return {
        valid: false,
        error: `File type '${file.type}' is not allowed. Allowed types: ${this.configService.ui.allowedFileTypes.join(', ')}`
      };
    }

    // Check for empty files
    if (file.size === 0) {
      return { valid: false, error: 'Cannot upload empty files' };
    }

    // Additional security checks
    const securityCheck = this.performSecurityChecks(file);
    if (!securityCheck.valid) {
      return securityCheck;
    }

    this.logger.debug('File validation passed', { fileName: file.name });
    return { valid: true };
  }

  validateChunk(chunk: UploadChunk): { valid: boolean; error?: string } {
    this.logger.debug('Validating chunk', {
      fileId: chunk.fileId,
      chunkIndex: chunk.chunkIndex,
      totalChunks: chunk.totalChunks,
      dataSize: chunk.data.size
    });

    // Check if chunk data exists
    if (!chunk.data) {
      return { valid: false, error: 'Chunk data is missing' };
    }

    // Check chunk index
    if (chunk.chunkIndex < 0 || chunk.chunkIndex >= chunk.totalChunks) {
      return {
        valid: false,
        error: `Invalid chunk index ${chunk.chunkIndex}. Must be between 0 and ${chunk.totalChunks - 1}`
      };
    }

    // Check chunk size
    const maxChunkSize = this.configService.upload.chunkSize;
    if (chunk.data.size > maxChunkSize) {
      return {
        valid: false,
        error: `Chunk size (${chunk.data.size}) exceeds maximum chunk size (${maxChunkSize})`
      };
    }

    // Check for empty chunks (except possibly the last chunk)
    if (chunk.data.size === 0 && chunk.chunkIndex < chunk.totalChunks - 1) {
      return { valid: false, error: 'Empty chunks are not allowed (except for the last chunk)' };
    }

    // Validate file ID format (should be UUID)
    if (!this.isValidUUID(chunk.fileId)) {
      return { valid: false, error: 'Invalid file ID format' };
    }

    // Validate file name
    if (!chunk.fileName || chunk.fileName.trim().length === 0) {
      return { valid: false, error: 'File name is required' };
    }

    this.logger.debug('Chunk validation passed', {
      fileId: chunk.fileId,
      chunkIndex: chunk.chunkIndex
    });

    return { valid: true };
  }

  private performSecurityChecks(file: File): { valid: boolean; error?: string } {
    // Check for suspicious file names
    const suspiciousPatterns = [
      /\.\./,  // Directory traversal
      /[<>:"|?*]/,  // Invalid filename characters
      /^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$/i,  // Windows reserved names
    ];

    for (const pattern of suspiciousPatterns) {
      if (pattern.test(file.name)) {
        return { valid: false, error: 'File name contains invalid or suspicious characters' };
      }
    }

    // Check file name length
    if (file.name.length > 255) {
      return { valid: false, error: 'File name is too long (maximum 255 characters)' };
    }

    // Check for null bytes in filename
    if (file.name.includes('\0')) {
      return { valid: false, error: 'File name contains null bytes' };
    }

    return { valid: true };
  }

  private isValidUUID(uuid: string): boolean {
    const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
    return uuidRegex.test(uuid);
  }

  /**
   * Get file type from file name extension
   */
  getFileTypeFromExtension(fileName: string): string {
    const extension = fileName.split('.').pop()?.toLowerCase();
    if (!extension) return 'application/octet-stream';

    const mimeTypes: Record<string, string> = {
      'txt': 'text/plain',
      'pdf': 'application/pdf',
      'doc': 'application/msword',
      'docx': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'jpg': 'image/jpeg',
      'jpeg': 'image/jpeg',
      'png': 'image/png',
      'gif': 'image/gif',
      'mp4': 'video/mp4',
      'mp3': 'audio/mpeg',
      'zip': 'application/zip',
      'json': 'application/json',
      'xml': 'application/xml'
    };

    return mimeTypes[extension] || 'application/octet-stream';
  }
}
