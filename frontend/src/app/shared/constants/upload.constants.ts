/**
 * Upload-related constants
 */
export const UPLOAD_CONSTANTS = {
  // File size limits
  MAX_FILE_SIZE_DEFAULT: 10 * 1024 * 1024 * 1024, // 10GB
  MIN_FILE_SIZE: 1, // 1 byte
  
  // Chunk settings
  DEFAULT_CHUNK_SIZE: 5 * 1024 * 1024, // 5MB
  MIN_CHUNK_SIZE: 1024 * 1024, // 1MB
  MAX_CHUNK_SIZE: 100 * 1024 * 1024, // 100MB
  
  // Retry settings
  DEFAULT_RETRY_ATTEMPTS: 3,
  MAX_RETRY_ATTEMPTS: 10,
  DEFAULT_RETRY_DELAY: 1000, // 1 second
  MAX_RETRY_DELAY: 60000, // 1 minute
  
  // Timeout settings
  DEFAULT_TIMEOUT: 30000, // 30 seconds
  MAX_TIMEOUT: 300000, // 5 minutes
  
  // Progress settings
  PROGRESS_UPDATE_INTERVAL: 100, // milliseconds
  
  // File validation
  MAX_FILENAME_LENGTH: 255,
  INVALID_FILENAME_CHARS: /[<>:"|?*\x00-\x1f]/,
  WINDOWS_RESERVED_NAMES: /^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$/i,
  
  // HTTP status codes
  HTTP_STATUS: {
    OK: 200,
    CREATED: 201,
    BAD_REQUEST: 400,
    UNAUTHORIZED: 401,
    FORBIDDEN: 403,
    NOT_FOUND: 404,
    REQUEST_TIMEOUT: 408,
    PAYLOAD_TOO_LARGE: 413,
    UNSUPPORTED_MEDIA_TYPE: 415,
    TOO_MANY_REQUESTS: 429,
    INTERNAL_SERVER_ERROR: 500,
    BAD_GATEWAY: 502,
    SERVICE_UNAVAILABLE: 503,
    GATEWAY_TIMEOUT: 504
  },
  
  // MIME types
  MIME_TYPES: {
    OCTET_STREAM: 'application/octet-stream',
    JSON: 'application/json',
    TEXT_PLAIN: 'text/plain',
    PDF: 'application/pdf',
    ZIP: 'application/zip'
  }
} as const;

/**
 * Upload status constants
 */
export const UPLOAD_STATUS = {
  PENDING: 'pending',
  UPLOADING: 'uploading',
  PAUSED: 'paused',
  COMPLETING: 'completing',
  COMPLETED: 'completed',
  FAILED: 'failed',
  CANCELLED: 'cancelled'
} as const;

/**
 * Upload event types
 */
export const UPLOAD_EVENTS = {
  STARTED: 'started',
  PROGRESS: 'progress',
  CHUNK_COMPLETED: 'chunkCompleted',
  PAUSED: 'paused',
  RESUMED: 'resumed',
  COMPLETED: 'completed',
  FAILED: 'failed',
  CANCELLED: 'cancelled'
} as const;

/**
 * Worker message types
 */
export const WORKER_MESSAGE_TYPES = {
  START: 'start',
  PAUSE: 'pause',
  RESUME: 'resume',
  CANCEL: 'cancel'
} as const;

/**
 * Log levels
 */
export const LOG_LEVELS = {
  ERROR: 'error',
  WARN: 'warn',
  INFO: 'info',
  DEBUG: 'debug'
} as const;
