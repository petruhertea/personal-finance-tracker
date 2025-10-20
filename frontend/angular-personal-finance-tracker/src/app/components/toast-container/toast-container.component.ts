import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, Toast } from '../../services/toast.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container position-fixed top-0 end-0 p-3" style="z-index: 9999;">
      <div *ngFor="let toast of toasts$ | async" 
           class="toast show mb-2"
           [class.bg-success]="toast.type === 'success'"
           [class.bg-danger]="toast.type === 'error'"
           [class.bg-warning]="toast.type === 'warning'"
           [class.bg-info]="toast.type === 'info'"
           role="alert">
        <div class="toast-header">
          <i class="bi me-2" 
             [class.bi-check-circle-fill]="toast.type === 'success'"
             [class.bi-exclamation-triangle-fill]="toast.type === 'error'"
             [class.bi-exclamation-circle-fill]="toast.type === 'warning'"
             [class.bi-info-circle-fill]="toast.type === 'info'"></i>
          <strong class="me-auto">
            {{ toast.type === 'success' ? 'Success' : 
               toast.type === 'error' ? 'Error' : 
               toast.type === 'warning' ? 'Warning' : 'Info' }}
          </strong>
          <button type="button" 
                  class="btn-close" 
                  (click)="toastService.remove(toast.id)"></button>
        </div>
        <div class="toast-body text-white">
          {{ toast.message }}
        </div>
      </div>
    </div>
  `,
  styles: [`
    .toast {
      min-width: 300px;
      animation: slideIn 0.3s ease;
    }
    
    @keyframes slideIn {
      from {
        transform: translateX(100%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }
    
    .toast-header {
      background: rgba(255, 255, 255, 0.1);
      color: white;
      border-bottom: 1px solid rgba(255, 255, 255, 0.2);
    }
    
    .btn-close {
      filter: brightness(0) invert(1);
    }
  `]
})
export class ToastContainerComponent {
  toasts$: Observable<Toast[]>;

  constructor(public toastService: ToastService) {
    this.toasts$ = toastService.toasts;
  }
}