// auth.service.ts - Fully refactored with refresh token support
import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError, of, timer } from 'rxjs';
import { tap, catchError, switchMap, shareReplay } from 'rxjs';
import { User } from '../common/user';
import { UserResponse } from '../common/user-response';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  clearUserState() {
    this.currentUserSubject.next(null);
  }

  private authUrl = `${environment.apiUrl}/auth`;
  private userUrl = `${environment.apiUrl}/users`;
  
  private currentUserSubject = new BehaviorSubject<UserResponse | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  
  private refreshTokenInProgress = false;
  private refreshTokenSubject = new BehaviorSubject<boolean>(false);

  constructor(private http: HttpClient) { }

  /**
   * Auto-login: Check if user has valid session
   */
  autoLogin(): Observable<UserResponse | null> {
    if (this.currentUserSubject.value) {
      return of(this.currentUserSubject.value);
    }

    return this.http.get<UserResponse>(`${this.userUrl}/me`, { 
      withCredentials: true 
    }).pipe(
      tap(user => {
        console.log('✅ Auto-login successful:', user.username);
        this.currentUserSubject.next(user);
      }),
      catchError((error: HttpErrorResponse) => {
        console.log('⚠️ Auto-login failed, clearing user state');
        this.currentUserSubject.next(null);
        return of(null);
      })
    );
  }

  /**
   * Login with credentials
   */
  login(credentials: { username: string, password: string }): Observable<UserResponse> {
    return this.http.post<UserResponse>(
      `${this.authUrl}/login`, 
      credentials,
      { withCredentials: true }
    ).pipe(
      tap(user => {
        console.log('✅ Login successful:', user.username);
        this.currentUserSubject.next(user);
      }),
      catchError((error: HttpErrorResponse) => {
        console.error('❌ Login failed:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Register new user
   */
  register(user: User): Observable<string> {
    return this.http.post<string>(
      `${this.authUrl}/register`, 
      user, 
      { responseType: 'text' as 'json' }
    );
  }

  /**
   * Refresh access token using refresh token
   */
  refreshToken(): Observable<boolean> {
    // If already refreshing, wait for completion
    if (this.refreshTokenInProgress) {
      return this.refreshTokenSubject.asObservable().pipe(
        switchMap(success => success ? of(true) : throwError(() => new Error('Refresh failed')))
      );
    }

    this.refreshTokenInProgress = true;
    console.log('🔄 Refreshing access token...');

    return this.http.post(
      `${this.authUrl}/refresh`,
      {},
      { 
        withCredentials: true,
        responseType: 'text'
      }
    ).pipe(
      tap(() => {
        console.log('✅ Token refreshed successfully');
        this.refreshTokenInProgress = false;
        this.refreshTokenSubject.next(true);
      }),
      switchMap(() => of(true)),
      catchError((error: HttpErrorResponse) => {
        console.error('❌ Token refresh failed:', error);
        this.refreshTokenInProgress = false;
        this.refreshTokenSubject.next(false);
        this.handleAuthError();
        return throwError(() => error);
      }),
      shareReplay(1) // Share the result with multiple subscribers
    );
  }

  /**
   * Get current user data
   */
  getUser(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.userUrl}/me`, {
      withCredentials: true
    }).pipe(
      tap(user => this.currentUserSubject.next(user)),
      catchError((error: HttpErrorResponse) => {
        console.error('Failed to get user:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Logout user
   */
  logout(): Observable<string> {
    return this.http.post<string>(
      `${this.authUrl}/logout`, 
      {}, 
      { 
        withCredentials: true,
        responseType: 'text' as 'json'
      }
    ).pipe(
      tap(() => {
        console.log('✅ Logout successful');
        this.handleAuthError();
      }),
      catchError((error: HttpErrorResponse) => {
        console.error('❌ Logout failed:', error);
        // Clear state even if logout request fails
        this.handleAuthError();
        return throwError(() => error);
      })
    );
  }

  /**
   * Handle authentication errors (clear state)
   */
  private handleAuthError(): void {
    this.currentUserSubject.next(null);
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.currentUserSubject.value !== null;
  }

  /**
   * Get current user value (synchronous)
   */
  getCurrentUser(): UserResponse | null {
    return this.currentUserSubject.value;
  }
}