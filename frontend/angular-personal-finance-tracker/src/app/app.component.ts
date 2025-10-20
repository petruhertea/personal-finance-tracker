import { Component, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';
import { CommonModule, NgIf } from '@angular/common';
import { Observable } from 'rxjs';
import { UserResponse } from './common/user-response';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, NgIf, CommonModule, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})

// app.component.ts
export class AppComponent implements OnInit {
  title = 'angular-personal-finance-tracker';
  user$: Observable<UserResponse | null>;
  isCollapsed = false;
  isMobile = false;
  isSidebarOpen = false; // For mobile overlay

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

    this.user$.subscribe(user => {
      if (!user) {
        this.router.navigate(['/login']);
      }
    });
  }

  checkScreenSize() {
    this.isMobile = window.innerWidth < 768;
    if (!this.isMobile && this.isSidebarOpen) {
      this.isSidebarOpen = false;
    }
  }

  toggleSidebar() {
    if (this.isMobile) {
      this.isSidebarOpen = !this.isSidebarOpen;
    } else {
      this.isCollapsed = !this.isCollapsed;
    }
  }

  closeSidebar() {
    if (this.isMobile) {
      this.isSidebarOpen = false;
    }
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
