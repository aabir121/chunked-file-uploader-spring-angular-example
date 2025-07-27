import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IUploadStrategy } from '../interfaces/upload.interfaces';
import { UploadChunk } from '../../models/upload.model';
import { LoggerService } from '../logging/logger.service';

@Injectable({
  providedIn: 'root'
})
export class MultipartUploadStrategy implements IUploadStrategy {
  private logger = this.loggerService.createLogger('MultipartUploadStrategy');

  constructor(
    private http: HttpClient,
    private loggerService: LoggerService
  ) {}

  uploadChunk(chunk: UploadChunk, uploadUrl: string, signal?: AbortSignal): Observable<any> {
    const formData = new FormData();

    // Convert Blob to File to ensure proper multipart/form-data content type
    const chunkFile = new File([chunk.data], `${chunk.fileName}.chunk${chunk.chunkIndex}`, {
      type: 'application/octet-stream'
    });

    // Ensure 'file' is appended first for backend @RequestPart order
    formData.append('file', chunkFile);
    formData.append('fileId', chunk.fileId);
    formData.append('chunkNumber', chunk.chunkIndex.toString());
    formData.append('totalChunks', chunk.totalChunks.toString());

    this.logger.debug('Uploading chunk via Multipart strategy', {
      fileId: chunk.fileId,
      chunkNumber: chunk.chunkIndex,
      totalChunks: chunk.totalChunks,
      fileName: chunk.fileName,
      chunkSize: chunk.data.size,
      chunkFileName: chunkFile.name,
      chunkFileType: chunkFile.type
    });

    // Log FormData contents for debugging
    this.logger.debug('FormData contents:');
    for (const [key, value] of formData.entries()) {
      if (value instanceof File) {
        this.logger.debug(`${key}: File: ${value.name} (${value.size} bytes, ${value.type})`);
      } else {
        this.logger.debug(`${key}: ${value}`);
      }
    }

    const options: any = {};
    if (signal) {
      options.signal = signal;
    }

    return this.http.post(uploadUrl, formData, options);
  }

  getName(): string {
    return 'Multipart Upload';
  }

  getDescription(): string {
    return 'Uploads chunks using multipart/form-data format, compatible with most backends';
  }
}
