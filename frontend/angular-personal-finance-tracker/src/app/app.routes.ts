// app.routes.ts
import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { LoginComponent } from './components/login/login.component';
import { AuthGuard } from './auth.guard';
import { RegisterComponent } from './components/register/register.component';
import { ProfileComponent } from './components/profile/profile.component';
import { TransactionComponent } from './components/transaction/transaction.component';
import { BudgetComponent } from './components/budget/budget.component';
import { BankStatementImportComponent } from './components/bank-statement-import/bank-statement-import.component';
import { EmailVerificationComponent } from './components/email-verification/email-verification.component';

export const routes: Routes = [
  { path: 'import', component: BankStatementImportComponent, canActivate: [AuthGuard] },
  { path: 'verify-email', component: EmailVerificationComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard] },
  { path: 'profile', component: ProfileComponent, canActivate: [AuthGuard] },
  { path: 'transactions', component: TransactionComponent, canActivate: [AuthGuard] },
  { path: 'budgets', component: BudgetComponent, canActivate: [AuthGuard] },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' }, 
  { path: '**', redirectTo: 'login' }
];
