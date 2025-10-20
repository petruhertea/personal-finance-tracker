import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-error-message',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="alert alert-danger alert-dismissible fade show" role="alert" *ngIf="message">
      <i class="bi bi-exclamation-triangle-fill me-2"></i>
      <strong>Error:</strong> {{ message }}
      <button type="button" class="btn-close" (click)="onDismiss()" aria-label="Close"></button>
    </div>
  `,
  styles: [`
    .alert {
      margin-bottom: 1rem;
      animation: slideDown 0.3s ease;
    }
    
    @keyframes slideDown {
      from {
        opacity: 0;
        transform: translateY(-10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
  `]
})
export class ErrorMessageComponent {
  @Input() message: string = '';
  @Output() dismiss = new EventEmitter<void>();

  onDismiss() {
    this.message = '';
    this.dismiss.emit();
  }
}