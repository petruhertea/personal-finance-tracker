import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="loading-container" [class.fullscreen]="fullscreen">
      <div class="spinner-border" [class]="spinnerClass" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
      <p class="mt-3 text-muted" *ngIf="message">{{ message }}</p>
    </div>
  `,
  styles: [`
    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 3rem;
    }
    
    .loading-container.fullscreen {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(255, 255, 255, 0.9);
      z-index: 9999;
    }
    
    .spinner-border {
      width: 3rem;
      height: 3rem;
    }
  `]
})
export class LoadingSpinnerComponent {
  @Input() message: string = '';
  @Input() fullscreen: boolean = false;
  @Input() spinnerClass: string = 'text-primary';
}