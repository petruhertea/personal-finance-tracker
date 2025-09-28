import { Component, OnInit } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';
import { StoredUser } from './common/stored-user';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit{
  title = 'angular-personal-finance-tracker';
  user: StoredUser | null = null;
  
  constructor(private authService: AuthService, private router: Router){}

  ngOnInit(): void {
    this.authService.getUser().subscribe((storedUser: StoredUser | null) => {
      if (!storedUser) {
        this.router.navigate(['/login']); // redirect dacă nu e logat
      } else {
        this.user = storedUser;
      }
    });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
