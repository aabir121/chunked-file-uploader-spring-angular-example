import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UploadChunk } from '../models/upload.model';

@Injectable({
  providedIn: 'root'
})
export class BinaryUploadService {

  constructor(private http: HttpClient) { }

  /**
   * Upload chunk using binary endpoint (more efficient for large chunks)
   * Sends chunk data as raw binary with metadata in headers
   */
  uploadChunkBinary(chunk: UploadChunk, uploadUrl: string): Observable<any> {
    // Prepare headers with chunk metadata
    const headers = new HttpHeaders({
      'Content-Type': 'application/octet-stream',
      'X-File-Id': chunk.fileId,
      'X-Chunk-Number': chunk.chunkIndex.toString(),
      'X-Total-Chunks': chunk.totalChunks.toString(),
      'X-File-Name': chunk.fileName
    });

    console.log('Uploading chunk via Binary endpoint:', {
      fileId: chunk.fileId,
      chunkNumber: chunk.chunkIndex,
      totalChunks: chunk.totalChunks,
      fileName: chunk.fileName,
      chunkSize: chunk.data.size,
      contentType: 'application/octet-stream'
    });

    // Send the raw blob data directly
    return this.http.post(`${uploadUrl}/binary`, chunk.data, { 
      headers: headers,
      responseType: 'text' // Expect empty response
    });
  }

  /**
   * Upload chunk using multipart endpoint (compatible with existing backend)
   * Converts blob to file and sends as multipart/form-data
   */
  uploadChunkMultipart(chunk: UploadChunk, uploadUrl: string): Observable<any> {
    const formData = new FormData();
    
    // Convert Blob to File to ensure proper multipart/form-data content type
    const chunkFile = new File([chunk.data], `${chunk.fileName}.chunk${chunk.chunkIndex}`, {
      type: 'application/octet-stream'
    });
    
    formData.append('file', chunkFile);
    formData.append('fileId', chunk.fileId);
    formData.append('chunkNumber', chunk.chunkIndex.toString());
    formData.append('totalChunks', chunk.totalChunks.toString());

    console.log('Uploading chunk via Multipart endpoint:', {
      fileId: chunk.fileId,
      chunkNumber: chunk.chunkIndex,
      totalChunks: chunk.totalChunks,
      fileName: chunk.fileName,
      chunkSize: chunk.data.size,
      chunkFileName: chunkFile.name,
      chunkFileType: chunkFile.type
    });

    return this.http.post(uploadUrl, formData);
  }
}
