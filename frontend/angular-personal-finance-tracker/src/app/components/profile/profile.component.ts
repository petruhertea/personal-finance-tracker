// profile.component.ts - With real API integration
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ProfileService, AccountStats } from '../../services/profile.service';
import { UserResponse } from '../../common/user-response';
import { finalize, Subject, takeUntil } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-profile',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit, OnDestroy {
  currentUser: UserResponse | null = null;

  // Forms
  emailForm!: FormGroup;
  passwordForm!: FormGroup;

  // profile.component.ts - Add this to show email status
  emailVerificationStatus: 'verified' | 'unverified' | 'pending' = 'unverified';

  // UI State
  activeSection: 'overview' | 'security' | 'preferences' | 'data' = 'overview';
  isEditingEmail = false;
  isEditingPassword = false;
  isLoading = false;

  // Messages
  successMessage = '';
  errorMessage = '';

  // Stats
  accountStats: AccountStats = {
    memberSince: new Date(),
    totalTransactions: 0,
    totalBudgets: 0,
    lastLogin: new Date()
  };

  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private profileService: ProfileService,
    private fb: FormBuilder,
    private router: Router,
    private http: HttpClient
  ) { }

  ngOnInit(): void {
    this.loadUserData();
    this.loadAccountStats();

    // ✅ Check email verification status
    this.checkEmailVerificationStatus();
  }

  checkEmailVerificationStatus(): void {
    if (this.currentUser?.email) {
      // You'll need to add this field to UserResponse
      this.emailVerificationStatus = this.currentUser.emailVerified ? 'verified' : 'unverified';
    }
  }

  resendVerificationEmail(): void {
    if (!this.currentUser?.email) return;

    this.isLoading = true;
    this.http.post(
      `${environment.apiUrl}/email/resend`,
      { email: this.currentUser.email },
      { responseType: 'text', withCredentials: true }
    ).pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: () => {
        this.successMessage = 'Verification email sent! Please check your inbox.';
      },
      error: (err: any) => {
        this.errorMessage = err.error || 'Failed to send verification email';
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadUserData(): void {
    this.authService.getUser().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: user => {
        this.currentUser = user;
        this.initForms();
      },
      error: err => {
        console.error('Error loading user data:', err);
        this.errorMessage = 'Failed to load user data';
      }
    });
  }

  loadAccountStats(): void {
    this.profileService.getAccountStats().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: stats => {
        this.accountStats = {
          ...stats,
          memberSince: new Date(stats.memberSince),
          lastLogin: new Date(stats.lastLogin)
        };
      },
      error: err => {
        console.error('Error loading account stats:', err);
      }
    });
  }

  initForms(): void {
    this.emailForm = this.fb.group({
      email: [this.currentUser?.email || '', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });

    this.passwordForm = this.fb.group({
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(group: FormGroup): { [key: string]: boolean } | null {
    const newPassword = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return newPassword === confirmPassword ? null : { mismatch: true };
  }

  setActiveSection(section: 'overview' | 'security' | 'preferences' | 'data'): void {
    this.activeSection = section;
    this.clearMessages();
  }

  toggleEditEmail(): void {
    this.isEditingEmail = !this.isEditingEmail;
    if (!this.isEditingEmail) {
      this.emailForm.reset({ email: this.currentUser?.email });
    }
  }

  updateEmail(): void {
    if (this.emailForm.invalid) return;

    this.isLoading = true;
    this.clearMessages();

    this.profileService.updateEmail(this.emailForm.value).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (updatedUser) => {
        this.isLoading = false;
        this.successMessage = 'Email updated successfully!';
        this.isEditingEmail = false;
        this.currentUser = updatedUser;
        this.loadUserData();
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error || 'Failed to update email';
      }
    });
  }

  toggleEditPassword(): void {
    this.isEditingPassword = !this.isEditingPassword;
    if (!this.isEditingPassword) {
      this.passwordForm.reset();
    }
  }

  changePassword(): void {
    if (this.passwordForm.invalid) return;

    this.isLoading = true;
    this.clearMessages();

    const request = {
      currentPassword: this.passwordForm.value.currentPassword,
      newPassword: this.passwordForm.value.newPassword
    };

    this.profileService.changePassword(request).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Password changed successfully! You will be logged out shortly.';
        this.isEditingPassword = false;
        this.passwordForm.reset();

        // Auto logout after password change
        setTimeout(() => {
          this.authService.logout().subscribe(() => {
            this.router.navigate(['/login']);
          });
        }, 2000);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error || 'Failed to change password';
      }
    });
  }

  logoutAllDevices(): void {
    if (!confirm('This will log you out from all devices. Continue?')) return;

    this.isLoading = true;
    this.clearMessages();

    this.profileService.logoutAllDevices().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Logged out from all devices successfully! Redirecting...';

        setTimeout(() => {
          this.authService.logout().subscribe(() => {
            this.router.navigate(['/login']);
          });
        }, 2000);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Failed to logout from all devices';
      }
    });
  }

  exportData(): void {
    this.isLoading = true;
    this.clearMessages();

    this.profileService.exportData().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (blob) => {
        this.isLoading = false;
        this.successMessage = 'Data export complete! Downloading...';

        // Create download link
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `user-data-export-${Date.now()}.json`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = 'Failed to export data';
      }
    });
  }

  deleteAccount(): void {
    const confirmation = prompt(
      'This action is PERMANENT and cannot be undone.\n\n' +
      'Type "DELETE MY ACCOUNT" to confirm:'
    );

    if (confirmation !== 'DELETE MY ACCOUNT') {
      return;
    }

    const password = prompt('Enter your password to confirm:');
    if (!password) {
      return;
    }

    this.isLoading = true;
    this.clearMessages();

    this.profileService.deleteAccount({ password }).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Account deleted successfully. Goodbye!';

        setTimeout(() => {
          this.authService.logout().subscribe(() => {
            this.router.navigate(['/login']);
          });
        }, 3000);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error || 'Failed to delete account';
      }
    });
  }

  clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }

  getDaysSinceMember(): number {
    const diffTime = Math.abs(new Date().getTime() - this.accountStats.memberSince.getTime());
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  }
}