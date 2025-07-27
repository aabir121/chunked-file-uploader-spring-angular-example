/// <reference lib="webworker" />
import { UploadChunk, UploadWorkerMessage, UploadWorkerResponse, UploadWorkerMessagePayload } from './models/upload.model';

interface UploadTask {
  fileId: string;
  file: File;
  chunkSize: number;
  totalChunks: number;
  uploadUrl: string;
  uploadedChunks: Set<number>;
  missingChunks?: Set<number>;
  isPaused: boolean;
  controller: AbortController;
  startTime: number;
  uploadedBytes: number;
}

const uploadTasks = new Map<string, UploadTask>();

addEventListener('message', async ({ data }: { data: UploadWorkerMessage }) => {
  switch (data.type) {
    case 'start':
      await startUpload(data.payload);
      break;
    case 'pause':
      pauseUpload(data.payload.fileId);
      break;
    case 'resume':
      await startUpload(data.payload);
      break;
    case 'cancel':
      cancelUpload(data.payload.fileId);
      break;
  }
});

async function startUpload(payload: UploadWorkerMessagePayload): Promise<void> {
  const { fileId, file, chunkSize, totalChunks, uploadUrl, uploadedChunks } = payload;

  if (!file || !chunkSize || !totalChunks || !uploadUrl) {
    postMessageToMain({
      type: 'failed',
      payload: { fileId, error: 'Missing required upload parameters' }
    });
    return;
  }

  let task = uploadTasks.get(fileId);
  if (!task) {
    const uploadedChunksSet = new Set(uploadedChunks || []);
    const missingChunksSet = new Set<number>();

    // Calculate missing chunks
    for (let i = 0; i < totalChunks; i++) {
      if (!uploadedChunksSet.has(i)) {
        missingChunksSet.add(i);
      }
    }

    task = {
      fileId,
      file,
      chunkSize,
      totalChunks,
      uploadUrl,
      uploadedChunks: uploadedChunksSet,
      missingChunks: missingChunksSet,
      isPaused: false,
      controller: new AbortController(),
      startTime: Date.now(),
      uploadedBytes: uploadedChunksSet.size * chunkSize
    };
    uploadTasks.set(fileId, task);
  } else {
    task.isPaused = false;
    task.controller = new AbortController(); // New controller for resume

    // Recalculate missing chunks
    const missingChunksSet = new Set<number>();
    for (let i = 0; i < task.totalChunks; i++) {
      if (!task.uploadedChunks.has(i)) {
        missingChunksSet.add(i);
      }
    }
    task.missingChunks = missingChunksSet;
  }

  postMessageToMain({
    type: 'progress',
    payload: {
      fileId,
      progress: calculateProgress(task),
      uploadedChunks: Array.from(task.uploadedChunks)
    }
  });

  // Upload only missing chunks
  const missingChunksArray = Array.from(task.missingChunks || []);
  console.log(`Uploading ${missingChunksArray.length} missing chunks for file ${fileId}`);

  for (const chunkIndex of missingChunksArray) {
    if (task.isPaused) {
      return;
    }

    try {
      await uploadMissingChunk(task, chunkIndex);
      task.uploadedChunks.add(chunkIndex);
      task.missingChunks?.delete(chunkIndex);

      // Update uploaded bytes
      const chunkSize = calculateActualChunkSize(task, chunkIndex);
      task.uploadedBytes += chunkSize;

      postMessageToMain({
        type: 'chunkCompleted',
        payload: { fileId: task.fileId, chunkIndex }
      });

      postMessageToMain({
        type: 'progress',
        payload: {
          fileId,
          progress: calculateProgress(task),
          uploadedChunks: Array.from(task.uploadedChunks),
          uploadedBytes: task.uploadedBytes,
          uploadSpeed: calculateUploadSpeed(task),
          estimatedRemainingTime: calculateEstimatedRemainingTime(task)
        }
      });
    } catch (error: any) {
      if (error.name === 'AbortError') {
        console.log(`Upload for ${fileId} was aborted.`);
        return;
      }
      console.error(`Error uploading chunk ${chunkIndex.toString()} for file ${fileId}:`, error);
      postMessageToMain({ type: 'failed', payload: { fileId, error: error.message } });
      return;
    }
  }

  if (task.uploadedChunks.size === task.totalChunks) {
    // All chunks uploaded, now call the complete API to finalize the file
    postMessageToMain({ type: 'completing', payload: { fileId } });

    try {
      await completeUpload(task.fileId, task.uploadUrl);
      postMessageToMain({ type: 'completed', payload: { fileId } });
      uploadTasks.delete(fileId);
    } catch (error: any) {
      console.error(`Error completing upload for file ${fileId}:`, error);
      postMessageToMain({ type: 'failed', payload: { fileId, error: error.message } });
      return;
    }
  }
}

function pauseUpload(fileId: string): void {
  const task = uploadTasks.get(fileId);
  if (task) {
    task.isPaused = true;
    task.controller.abort(); // Abort ongoing fetch requests
  }
}

function cancelUpload(fileId: string): void {
  const task = uploadTasks.get(fileId);
  if (task) {
    task.isPaused = true; // Ensure no new chunks are sent
    task.controller.abort(); // Abort ongoing fetch requests
    uploadTasks.delete(fileId);
  }
}

async function uploadChunkToServer(chunk: UploadChunk, uploadUrl: string, signal?: AbortSignal): Promise<void> {
  const formData = new FormData();

  // Convert Blob to File to ensure proper multipart/form-data content type
  const chunkFile = new File([chunk.data], `${chunk.fileName}.chunk${chunk.chunkIndex}`, {
    type: 'application/octet-stream'
  });

  formData.append('fileId', chunk.fileId);
  formData.append('chunkNumber', chunk.chunkIndex.toString());
  formData.append('totalChunks', chunk.totalChunks.toString());
  formData.append('fileName', chunk.fileName); // Send original filename separately
  formData.append('file', chunkFile);

  console.log('Uploading chunk:', {
    fileId: chunk.fileId,
    chunkNumber: chunk.chunkIndex,
    totalChunks: chunk.totalChunks,
    fileName: chunk.fileName,
    chunkSize: chunk.data.size,
    chunkFileName: chunkFile.name,
    chunkFileType: chunkFile.type
  });

  // Log FormData contents for debugging
  console.log('FormData contents:');
  for (const [key, value] of formData.entries()) {
    if (value instanceof File) {
      console.log(`${key}:`, `File: ${value.name} (${value.size} bytes, ${value.type})`);
    } else {
      console.log(`${key}:`, value);
    }
  }

  const response = await fetch(uploadUrl, {
    method: 'POST',
    body: formData,
    signal: signal,
    // Don't set Content-Type header - let the browser set it with boundary
  });

  if (!response.ok) {
    const errorText = await response.text();
    console.error('Upload failed:', response.status, response.statusText, errorText);
    console.error('Response headers:', Object.fromEntries(response.headers.entries()));
    throw new Error(`Failed to upload chunk ${chunk.chunkIndex}: ${response.statusText} - ${errorText}`);
  }
}

async function completeUpload(fileId: string, uploadUrl: string): Promise<void> {
  console.log('Completing upload for fileId:', fileId);

  const response = await fetch(`${uploadUrl}/${fileId}/complete`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    const errorText = await response.text();
    console.error('Complete upload failed:', response.status, response.statusText, errorText);
    console.error('Response headers:', Object.fromEntries(response.headers.entries()));
    throw new Error(`Failed to complete upload for ${fileId}: ${response.statusText} - ${errorText}`);
  }

  console.log('Upload completed successfully for fileId:', fileId);
}

function calculateProgress(task: UploadTask): number {
  return (task.uploadedChunks.size / task.totalChunks) * 100;
}

function postMessageToMain(message: UploadWorkerResponse): void {
  postMessage(message);
}

async function uploadMissingChunk(task: UploadTask, chunkIndex: number): Promise<void> {
  const start = chunkIndex * task.chunkSize;
  const end = Math.min(start + task.chunkSize, task.file.size);
  const chunkData = task.file.slice(start, end);

  const chunk: UploadChunk = {
    fileId: task.fileId,
    chunkIndex: chunkIndex,
    data: chunkData,
    totalChunks: task.totalChunks,
    fileName: task.file.name,
    fileSize: task.file.size
  };

  await uploadChunkToServer(chunk, task.uploadUrl, task.controller.signal);
}

function calculateActualChunkSize(task: UploadTask, chunkIndex: number): number {
  const start = chunkIndex * task.chunkSize;
  const end = Math.min(start + task.chunkSize, task.file.size);
  return end - start;
}

function calculateUploadSpeed(task: UploadTask): number {
  const elapsedTime = Date.now() - task.startTime;
  if (elapsedTime === 0) return 0;
  return (task.uploadedBytes / elapsedTime) * 1000; // bytes per second
}

function calculateEstimatedRemainingTime(task: UploadTask): number {
  const uploadSpeed = calculateUploadSpeed(task);
  if (uploadSpeed === 0) return 0;

  const remainingBytes = (task.file.size - task.uploadedBytes);
  return remainingBytes / uploadSpeed * 1000; // milliseconds
}

