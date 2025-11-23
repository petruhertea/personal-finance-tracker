import { HttpClient } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-email-verification',
  imports: [CommonModule],
  templateUrl: './email-verification.component.html',
  styleUrl: './email-verification.component.css'
})
export class EmailVerificationComponent implements OnInit {
  isVerifying = true;
  verificationSuccess = false;
  verificationError = false;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    
    if (!token) {
      this.verificationError = true;
      this.errorMessage = 'Invalid verification link';
      this.isVerifying = false;
      return;
    }

    this.verifyEmail(token);
  }

  verifyEmail(token: string): void {
    this.http.get(`${environment.apiUrl}/email/verify?token=${token}`, {
      responseType: 'text'
    }).subscribe({
      next: (response) => {
        this.isVerifying = false;
        this.verificationSuccess = true;
      },
      error: (error) => {
        this.isVerifying = false;
        this.verificationError = true;
        this.errorMessage = error.error || 'Verification failed. The link may have expired.';
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
