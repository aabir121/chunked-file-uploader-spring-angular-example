// This file can be replaced during build by using the `fileReplacements` array.
// `ng build` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  production: false,
  api: {
    baseUrl: 'http://localhost:8080',
    uploadEndpoint: '/upload',
    binaryUploadEndpoint: '/upload/binary',
    statusEndpoint: '/upload',
    resumeEndpoint: '/upload/{fileId}/resume',
    resumableUploadsEndpoint: '/upload/resumable'
  },
  upload: {
    chunkSize: 5 * 1024 * 1024, // 5MB chunks
    maxConcurrentUploads: 3,
    retryAttempts: 3,
    retryDelay: 1000, // 1 second
    timeoutMs: 30000 // 30 seconds
  },
  ui: {
    progressUpdateInterval: 100, // milliseconds
    autoCloseModalDelay: 2000, // 2 seconds
    maxFileSize: 10 * 1024 * 1024 * 1024, // 10GB
    allowedFileTypes: ['*'] // Allow all file types by default
  },
  logging: {
    level: 'debug' as const, // 'error', 'warn', 'info', 'debug'
    enableConsoleLogging: true,
    enableRemoteLogging: false
  }
};
