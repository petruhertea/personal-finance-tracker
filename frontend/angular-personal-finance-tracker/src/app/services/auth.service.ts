// src/app/auth/services/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, switchMap, tap } from 'rxjs';
import { User } from '../common/user';
import { StoredUser } from '../common/stored-user';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private authUrl = 'http://localhost:8080/api/auth'; // backend-ul tău
  private userUrl = 'http://localhost:8080/api/users'; 
  private tokenKey = 'jwt_token';
  private currentUser: BehaviorSubject<StoredUser | null> = new BehaviorSubject<StoredUser | null>(JSON.parse(localStorage.getItem('user') || 'null'));

  constructor(private http: HttpClient) { }

  login(credentials: { username: string, password: string }) {
    return this.http.post<{ token: string }>(`${this.authUrl}/login`, credentials).pipe(
      tap(response => {
        localStorage.setItem('jwt_token', response.token);
      }),
      switchMap(() => this.getUser()) // după login, ia și datele user-ului
    );
  }

  getUser() {
    
    return this.http.get<StoredUser>(`${this.userUrl}/me`).pipe(
      tap(user => this.currentUser.next(user))
    );
  }

  register(user: User): Observable<any> {
    return this.http.post(`${this.authUrl}/register`, user, { responseType: 'text' });
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    this.currentUser.next(null);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }


}
