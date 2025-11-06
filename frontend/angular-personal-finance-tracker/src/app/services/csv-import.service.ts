import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
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
export class CsvImportService {
  private apiUrl = `${environment.apiUrl}/import`;

  constructor(private http: HttpClient) {}

  importCsv(
    file: File,
    dateColumn: string = 'Date',
    amountColumn: string = 'Amount',
    descriptionColumn: string = 'Description',
    typeColumn?: string
  ): Observable<ImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('dateColumn', dateColumn);
    formData.append('amountColumn', amountColumn);
    formData.append('descriptionColumn', descriptionColumn);
    if (typeColumn) {
      formData.append('typeColumn', typeColumn);
    }

    return this.http.post<ImportResult>(`${this.apiUrl}/csv`, formData, {
      withCredentials: true
    });
  }
}
