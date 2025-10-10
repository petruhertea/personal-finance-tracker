import { Component, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';
import { UserResponse } from './common/stored-user';
import { CommonModule, NgIf } from '@angular/common';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, NgIf, CommonModule, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'angular-personal-finance-tracker';
  user$: Observable<UserResponse | null>;
  isCollapsed = false;

  links = [
    { path: '/dashboard', label: 'Home', icon: 'bi bi-house' },
    { path: '/transactions', label: 'Transactions', icon: 'bi bi-graph-up-arrow' },
    { path: '/budgets', label: 'Budgets', icon: 'bi bi-piggy-bank' },
    { path: '/profile', label: 'Profile', icon: 'bi bi-person' }
  ];


  constructor(private authService: AuthService, private router: Router) {
    this.user$ = this.authService.currentUser$;
  }

  ngOnInit(): void {
    // Redirect to login if the JWT is invalid or expired:
    this.user$.subscribe(user => {
      if (!user) {
        this.router.navigate(['/login']);
      }
    });
  }

  toggleSidebar() {
    this.isCollapsed = !this.isCollapsed;
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
