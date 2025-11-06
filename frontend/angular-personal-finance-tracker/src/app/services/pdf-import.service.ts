import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ImportResult {
  successCount: number;
  errors: string[];
  duplicateCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class PdfImportService {
  private apiUrl = `${environment.apiUrl}/import`;

  constructor(private http: HttpClient) {}

  importPdf(file: File, bankType: string = 'AUTO'): Observable<ImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('bankType', bankType);

    return this.http.post<ImportResult>(`${this.apiUrl}/pdf`, formData, {
      withCredentials: true
    });
  }
}