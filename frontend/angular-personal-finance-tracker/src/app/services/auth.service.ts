// src/app/auth/services/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, switchMap, tap } from 'rxjs';
import { User } from '../common/user';
import { UserResponse } from '../common/stored-user';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private authUrl = 'http://localhost:8080/api/auth';
  private userUrl = 'http://localhost:8080/api/users';
  private tokenKey = 'jwt_token';
  private currentUser: BehaviorSubject<UserResponse | null> = new BehaviorSubject<UserResponse | null>(null);
  currentUser$: Observable<UserResponse | null> = this.currentUser.asObservable();

  constructor(private http: HttpClient) { }

  login(credentials: { username: string, password: string }) {
    return this.http.post<{ token: string }>(`${this.authUrl}/login`, credentials).pipe(
      tap(response => {
        localStorage.setItem('jwt_token', response.token);
      }),
      switchMap(() => this.getUser()),
      tap(user => {
        this.currentUser.next(user);
      })
    );
  }

  getUser() {
    return this.http.get<UserResponse>(`${this.userUrl}/me`).pipe(
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

  isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expiry = payload.exp * 1000;
      return Date.now() > expiry;
    } catch {
      // token is expired
      return true;
    }
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    return token != null && !this.isTokenExpired(token);
  }



}
