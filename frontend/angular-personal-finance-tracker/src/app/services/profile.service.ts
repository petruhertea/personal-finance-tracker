// src/app/services/profile.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AccountStats {
  memberSince: Date;
  totalTransactions: number;
  totalBudgets: number;
  lastLogin: Date;
}

export interface EmailUpdateRequest {
  email: string;
  password: string;
}

export interface PasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
}

export interface DeleteAccountRequest {
  password: string;
}

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private apiUrl = `${environment.apiUrl}/profile`;

  constructor(private http: HttpClient) {}

  getAccountStats(): Observable<AccountStats> {
    return this.http.get<AccountStats>(`${this.apiUrl}/stats`, {
      withCredentials: true
    });
  }

  updateEmail(request: EmailUpdateRequest): Observable<any> {
    return this.http.put(`${this.apiUrl}/email`, request, {
      withCredentials: true
    });
  }

  changePassword(request: PasswordChangeRequest): Observable<string> {
    return this.http.put<string>(`${this.apiUrl}/password`, request, {
      withCredentials: true,
      responseType: 'text' as 'json'
    });
  }

  logoutAllDevices(): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/logout-all`, {}, {
      withCredentials: true,
      responseType: 'text' as 'json'
    });
  }

  exportData(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export`, {
      withCredentials: true,
      responseType: 'blob'
    });
  }

  deleteAccount(request: DeleteAccountRequest): Observable<string> {
    return this.http.delete<string>(`${this.apiUrl}/account`, {
      body: request,
      withCredentials: true,
      responseType: 'text' as 'json'
    });
  }
}