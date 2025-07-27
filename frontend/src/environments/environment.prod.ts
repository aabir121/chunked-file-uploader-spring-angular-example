export const environment = {
  production: true,
  api: {
    baseUrl: 'https://api.yourapp.com',
    uploadEndpoint: '/upload',
    binaryUploadEndpoint: '/upload/binary',
    statusEndpoint: '/upload',
    resumeEndpoint: '/upload/{fileId}/resume',
    resumableUploadsEndpoint: '/upload/resumable'
  },
  upload: {
    chunkSize: 5242880, // 5MB default
    maxConcurrentUploads: 3,
    retryAttempts: 3,
    retryDelay: 1000,
    timeoutMs: 30000
  },
  ui: {
    progressUpdateInterval: 100,
    autoCloseModalDelay: 2000,
    maxFileSize: 10737418240, // 10GB default
    allowedFileTypes: ['*']
  },
  logging: {
    level: 'info' as const,
    enableConsoleLogging: true,
    enableRemoteLogging: false
  }
};
