// auth.interceptor.ts - With automatic token refresh
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError, switchMap } from 'rxjs';
import { AuthService } from './services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Clone request with credentials
  const authReq = req.clone({
    withCredentials: true
  });

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // If 401 and not already on auth endpoints
      if (error.status === 401 && !req.url.includes('/auth/')) {
        // Try to refresh token
        return authService.refreshToken().pipe(
          switchMap(() => {
            // Retry original request after refresh
            return next(req.clone({ withCredentials: true }));
          }),
          catchError((refreshError) => {
            // Refresh failed, logout user
            console.error('Token refresh failed:', refreshError);
            authService.clearUserState();
            router.navigate(['/login']);
            return throwError(() => refreshError);
          })
        );
      }

      // For other errors, just pass through
      if (error.status === 403) {
        router.navigate(['/login']);
      }

      return throwError(() => error);
    })
  );
};