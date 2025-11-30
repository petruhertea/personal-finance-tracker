import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map } from 'rxjs';
import { BudgetDto } from '../common/budget-dto';
import { environment } from '../../environments/environment';
import { BudgetWithSpending } from '../common/budget-with-spending';

@Injectable({
  providedIn: 'root'
})
export class BudgetService {
  private budgetUrl = `${environment.apiUrl}/budgets`;
  private userUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  createBudget(budget: BudgetDto): Observable<BudgetDto> {
    return this.http.post<BudgetDto>(this.budgetUrl, budget);
  }

  updateBudget(id: number, budget: BudgetDto): Observable<void> {
    return this.http.put<void>(`${this.budgetUrl}/${id}`, budget);
  }

  deleteBudget(id: number): Observable<string> {
    return this.http.delete(`${this.budgetUrl}/${id}`,{ responseType: 'text' });
  }

  // ✅ New method: Get budgets with spending data
  getBudgetsWithSpending(userId: number): Observable<BudgetWithSpending[]> {
    return this.http.get<BudgetWithSpending[]>(`${this.userUrl}/${userId}/budgets`, {
      withCredentials: true
    });
  }
}
