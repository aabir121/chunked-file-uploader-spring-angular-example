import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IUploadStrategy } from '../interfaces/upload.interfaces';
import { UploadChunk } from '../../models/upload.model';
import { LoggerService } from '../logging/logger.service';

@Injectable({
  providedIn: 'root'
})
export class BinaryUploadStrategy implements IUploadStrategy {
  private logger = this.loggerService.createLogger('BinaryUploadStrategy');

  constructor(
    private http: HttpClient,
    private loggerService: LoggerService
  ) {}

  uploadChunk(chunk: UploadChunk, uploadUrl: string, signal?: AbortSignal): Observable<any> {
    // Prepare headers with chunk metadata
    const headers = new HttpHeaders({
      'Content-Type': 'application/octet-stream',
      'X-File-Id': chunk.fileId,
      'X-Chunk-Number': chunk.chunkIndex.toString(),
      'X-Total-Chunks': chunk.totalChunks.toString(),
      'X-File-Name': chunk.fileName,
      'X-File-Size': chunk.fileSize.toString()
    });

    this.logger.debug('Uploading chunk via Binary strategy', {
      fileId: chunk.fileId,
      chunkNumber: chunk.chunkIndex,
      totalChunks: chunk.totalChunks,
      fileName: chunk.fileName,
      chunkSize: chunk.data.size,
      contentType: 'application/octet-stream'
    });

    const options: any = {
      headers: headers,
      responseType: 'text' as 'json' // Expect empty response
    };

    if (signal) {
      options.signal = signal;
    }

    // Send the raw blob data directly
    return this.http.post(`${uploadUrl}/binary`, chunk.data, options);
  }

  getName(): string {
    return 'Binary Upload';
  }

  getDescription(): string {
    return 'Uploads chunks as raw binary data with metadata in headers, more efficient for large chunks';
  }
}
