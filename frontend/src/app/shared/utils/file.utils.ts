/**
 * File utility functions
 */
export class FileUtils {
  /**
   * Format file size in human-readable format
   */
  static formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  /**
   * Get file extension from filename
   */
  static getFileExtension(filename: string): string {
    return filename.split('.').pop()?.toLowerCase() || '';
  }

  /**
   * Get MIME type from file extension
   */
  static getMimeTypeFromExtension(extension: string): string {
    const mimeTypes: Record<string, string> = {
      // Text files
      'txt': 'text/plain',
      'csv': 'text/csv',
      'html': 'text/html',
      'css': 'text/css',
      'js': 'text/javascript',
      'json': 'application/json',
      'xml': 'application/xml',
      
      // Documents
      'pdf': 'application/pdf',
      'doc': 'application/msword',
      'docx': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'xls': 'application/vnd.ms-excel',
      'xlsx': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'ppt': 'application/vnd.ms-powerpoint',
      'pptx': 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
      
      // Images
      'jpg': 'image/jpeg',
      'jpeg': 'image/jpeg',
      'png': 'image/png',
      'gif': 'image/gif',
      'bmp': 'image/bmp',
      'webp': 'image/webp',
      'svg': 'image/svg+xml',
      'ico': 'image/x-icon',
      
      // Audio
      'mp3': 'audio/mpeg',
      'wav': 'audio/wav',
      'ogg': 'audio/ogg',
      'aac': 'audio/aac',
      'flac': 'audio/flac',
      
      // Video
      'mp4': 'video/mp4',
      'avi': 'video/x-msvideo',
      'mov': 'video/quicktime',
      'wmv': 'video/x-ms-wmv',
      'flv': 'video/x-flv',
      'webm': 'video/webm',
      'mkv': 'video/x-matroska',
      
      // Archives
      'zip': 'application/zip',
      'rar': 'application/vnd.rar',
      '7z': 'application/x-7z-compressed',
      'tar': 'application/x-tar',
      'gz': 'application/gzip',
      
      // Other
      'exe': 'application/vnd.microsoft.portable-executable',
      'dmg': 'application/x-apple-diskimage',
      'iso': 'application/x-iso9660-image'
    };

    return mimeTypes[extension.toLowerCase()] || 'application/octet-stream';
  }

  /**
   * Validate filename for security and compatibility
   */
  static validateFilename(filename: string): { valid: boolean; error?: string } {
    if (!filename || filename.trim().length === 0) {
      return { valid: false, error: 'Filename cannot be empty' };
    }

    if (filename.length > 255) {
      return { valid: false, error: 'Filename is too long (maximum 255 characters)' };
    }

    // Check for invalid characters
    const invalidChars = /[<>:"|?*\x00-\x1f]/;
    if (invalidChars.test(filename)) {
      return { valid: false, error: 'Filename contains invalid characters' };
    }

    // Check for Windows reserved names
    const baseName = filename.split('.')[0];
    const reservedNames = /^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$/i;
    if (reservedNames.test(baseName)) {
      return { valid: false, error: 'Filename uses a reserved name' };
    }

    // Check for null bytes
    if (filename.includes('\0')) {
      return { valid: false, error: 'Filename contains null bytes' };
    }

    // Check for directory traversal
    if (filename.includes('..')) {
      return { valid: false, error: 'Filename contains directory traversal sequences' };
    }

    return { valid: true };
  }

  /**
   * Sanitize filename by removing or replacing invalid characters
   */
  static sanitizeFilename(filename: string): string {
    return filename
      .replace(/[<>:"|?*\x00-\x1f]/g, '_') // Replace invalid chars with underscore
      .replace(/\.\./g, '_') // Replace directory traversal
      .replace(/^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$/i, '_$1') // Prefix reserved names
      .substring(0, 255); // Truncate to max length
  }

  /**
   * Check if file type is an image
   */
  static isImageFile(file: File): boolean {
    return file.type.startsWith('image/');
  }

  /**
   * Check if file type is a video
   */
  static isVideoFile(file: File): boolean {
    return file.type.startsWith('video/');
  }

  /**
   * Check if file type is an audio file
   */
  static isAudioFile(file: File): boolean {
    return file.type.startsWith('audio/');
  }

  /**
   * Check if file type is a document
   */
  static isDocumentFile(file: File): boolean {
    const documentTypes = [
      'application/pdf',
      'application/msword',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'application/vnd.ms-excel',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'application/vnd.ms-powerpoint',
      'application/vnd.openxmlformats-officedocument.presentationml.presentation',
      'text/plain',
      'text/csv'
    ];
    return documentTypes.includes(file.type);
  }

  /**
   * Generate a unique filename by appending timestamp
   */
  static generateUniqueFilename(originalFilename: string): string {
    const timestamp = Date.now();
    const extension = this.getFileExtension(originalFilename);
    const baseName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
    
    return extension 
      ? `${baseName}_${timestamp}.${extension}`
      : `${originalFilename}_${timestamp}`;
  }

  /**
   * Calculate file hash (simple implementation for client-side)
   */
  static async calculateFileHash(file: File): Promise<string> {
    const buffer = await file.arrayBuffer();
    const hashBuffer = await crypto.subtle.digest('SHA-256', buffer);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  }

  /**
   * Read file as data URL
   */
  static readFileAsDataURL(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });
  }

  /**
   * Read file as text
   */
  static readFileAsText(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = () => reject(reader.error);
      reader.readAsText(file);
    });
  }
}
