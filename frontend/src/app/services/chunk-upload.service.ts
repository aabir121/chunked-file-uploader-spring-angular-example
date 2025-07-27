import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { UploadChunk } from '../models/upload.model';

@Injectable({
  providedIn: 'root'
})
export class ChunkUploadService {

  constructor(private http: HttpClient) { }

  uploadChunk(chunk: UploadChunk, uploadUrl: string): Observable<any> {
    const formData = new FormData();

    // Convert Blob to File to ensure proper multipart/form-data content type
    const chunkFile = new File([chunk.data], `${chunk.fileName}.chunk${chunk.chunkIndex}`);

    // Ensure 'file' is appended first for backend @RequestPart order
    formData.append('file', chunkFile);
    formData.append('fileId', chunk.fileId);
    formData.append('chunkNumber', chunk.chunkIndex.toString());
    formData.append('totalChunks', chunk.totalChunks.toString());

    console.log('Uploading chunk via HttpClient:', {
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
      if ((value instanceof File)) {
        console.log(`${key}:`, `File: ${value.name} (${value.size} bytes, ${value.type})`);
      } else {
        console.log(`${key}:`, value);
      }
    }

    return this.http.post(uploadUrl, formData);
  }
}
