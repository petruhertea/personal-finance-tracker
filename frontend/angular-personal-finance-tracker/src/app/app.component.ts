// app.component.ts - Fixed redirect issue on refresh
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet, NavigationEnd } from '@angular/router';
import { AuthService } from './services/auth.service';
import { CommonModule } from '@angular/common';
import { Observable, Subject, filter, takeUntil } from 'rxjs';
import { UserResponse } from './common/user-response';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, CommonModule, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'angular-personal-finance-tracker';
  user$: Observable<UserResponse | null>;
  isCollapsed = false;
  isMobile = false;
  isSidebarOpen = false;
  isCheckingAuth = true;
  showSidebar = true;
  
  private destroy$ = new Subject<void>();

  links = [
    { path: '/dashboard', label: 'Home', icon: 'bi bi-house' },
    { path: '/transactions', label: 'Transactions', icon: 'bi bi-graph-up-arrow' },
    { path: '/budgets', label: 'Budgets', icon: 'bi bi-piggy-bank' },
    { path: '/profile', label: 'Profile', icon: 'bi bi-person' }
  ];

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    this.user$ = this.authService.currentUser$;
  }

  ngOnInit(): void {
    this.checkScreenSize();
    window.addEventListener('resize', () => this.checkScreenSize());

    // Handle route changes to hide sidebar on auth pages
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      takeUntil(this.destroy$)
    ).subscribe((event: any) => {
      const url = event.url;
      this.showSidebar = !url.includes('/login') && !url.includes('/register');
    });

    // Attempt auto-login BEFORE monitoring user state
    this.performAutoLogin();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private performAutoLogin(): void {
    this.isCheckingAuth = true;
    
    this.authService.autoLogin().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (user) => {
        this.isCheckingAuth = false;
        
        if (user) {
          console.log('✅ Auto-login successful');
          // If on login/register page, redirect to dashboard
          const currentUrl = this.router.url;
          if (currentUrl.includes('/login') || currentUrl.includes('/register') || currentUrl === '/') {
            this.router.navigate(['/dashboard']);
          }
          
          // NOW start monitoring user state changes (only after initial check)
          this.monitorAuthState();
        } else {
          console.log('⚠️ No valid session');
          // Only redirect to login if not already on auth pages
          const currentUrl = this.router.url;
          if (!currentUrl.includes('/login') && !currentUrl.includes('/register')) {
            this.router.navigate(['/login']);
          }
          
          // Monitor auth state even when not logged in
          this.monitorAuthState();
        }
      },
      error: (err) => {
        console.error('❌ Auto-login error:', err);
        this.isCheckingAuth = false;
        const currentUrl = this.router.url;
        if (!currentUrl.includes('/login') && !currentUrl.includes('/register')) {
          this.router.navigate(['/login']);
        }
        
        this.monitorAuthState();
      }
    });
  }

  /**
   * Monitor authentication state AFTER initial auto-login check
   * This prevents premature redirects during page refresh
   */
  private monitorAuthState(): void {
    this.user$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(user => {
      // Only redirect if:
      // 1. Not currently checking auth
      // 2. User is null
      // 3. We're on a protected page (showSidebar = true means protected page)
      if (!user && !this.isCheckingAuth && this.showSidebar) {
        console.log('⚠️ User session lost, redirecting to login');
        this.router.navigate(['/login']);
      }
    });
  }

  checkScreenSize(): void {
    this.isMobile = window.innerWidth < 768;
    if (!this.isMobile && this.isSidebarOpen) {
      this.isSidebarOpen = false;
    }
  }

  toggleSidebar(): void {
    if (this.isMobile) {
      this.isSidebarOpen = !this.isSidebarOpen;
    } else {
      this.isCollapsed = !this.isCollapsed;
    }
  }

  closeSidebar(): void {
    if (this.isMobile) {
      this.isSidebarOpen = false;
    }
  }

  logout(): void {
    this.authService.logout().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        console.log('✅ Logout successful');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        console.error('❌ Logout error:', err);
        this.router.navigate(['/login']);
      }
    });
  }
}