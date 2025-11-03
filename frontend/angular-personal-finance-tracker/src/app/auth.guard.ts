// auth.guard.ts - Fixed to work with auto-login
import { Injectable } from '@angular/core';
import { Router, UrlTree } from '@angular/router';
import { AuthService } from './services/auth.service';
import { Observable, map, take, tap, catchError, of, switchMap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard {
  constructor(
    private authService: AuthService, 
    private router: Router
  ) {}

  canActivate(): Observable<boolean | UrlTree> {
    // First check if we already have a user
    const currentUser = this.authService.getCurrentUser();
    
    if (currentUser) {
      // Already authenticated
      return of(true);
    }

    // No user yet, try auto-login first
    console.log('🔒 AuthGuard: Attempting auto-login...');
    
    return this.authService.autoLogin().pipe(
      take(1),
      map(user => {
        if (user) {
          console.log('✅ AuthGuard: Auto-login successful, allowing access');
          return true;
        } else {
          console.log('⚠️ AuthGuard: No valid session, redirecting to login');
          return this.router.createUrlTree(['/login']);
        }
      }),
      catchError(error => {
        console.error('❌ AuthGuard: Auto-login failed', error);
        return of(this.router.createUrlTree(['/login']));
      })
    );
  }
}