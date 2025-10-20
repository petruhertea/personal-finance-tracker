// app.config.ts
import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './auth.interceptor';

import { ReactiveFormsModule } from '@angular/forms';
import { errorInterceptor } from './error.interceptor';


export const appConfig: ApplicationConfig = {
  providers: [
    importProvidersFrom(
      ReactiveFormsModule
    ),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    importProvidersFrom()
  ]
};
