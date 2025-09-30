import { Component, OnInit } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';
import { UserResponse } from './common/stored-user';
import { CommonModule, NgIf } from '@angular/common';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, NgIf, CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'angular-personal-finance-tracker';
  user$: Observable<UserResponse | null>;

  constructor(private authService: AuthService, private router: Router) {
    this.user$ = this.authService.currentUser$;
  }

  ngOnInit(): void {
    // Dacă vrei redirect imediat când nu e user:
    this.user$.subscribe(user => {
      if (!user) {
        this.router.navigate(['/login']);
      }
    });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
