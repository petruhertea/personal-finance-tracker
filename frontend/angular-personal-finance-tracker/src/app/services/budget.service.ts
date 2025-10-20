import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map } from 'rxjs';
import { BudgetDto } from '../common/budget-dto';
import { Category } from '../common/category';
import { BudgetView } from '../common/budget-view';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class BudgetService {
  private budgetUrl = `${environment.apiUrl}/budgets`;
  private userUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  getBudgets(userId: number): Observable<BudgetDto[]> {
    return this.http.get<BudgetDto[]>(`${this.userUrl}/${userId}/budgets`);
  }

  createBudget(budget: BudgetDto): Observable<BudgetDto> {
    return this.http.post<BudgetDto>(this.budgetUrl, budget);
  }

  updateBudget(id: number, budget: BudgetDto): Observable<void> {
    return this.http.put<void>(`${this.budgetUrl}/${id}`, budget);
  }

  deleteBudget(id: number): Observable<string> {
    return this.http.delete(`${this.budgetUrl}/${id}`,{ responseType: 'text' });
  }

  /**
   * Fetch budgets and categories, then combine into BudgetView[]
   */
  getBudgetsWithCategories(userId: number): Observable<BudgetView[]> {
    return forkJoin({
      budgets: this.getBudgets(userId),
      categories: this.http.get<Category[]>(`${this.userUrl}/${userId}/categories`)
    }).pipe(
      map(({ budgets, categories }) => {
        return budgets.map(budget => {
          const category = categories.find(c => c.id === budget.categoryId);
          return {
            id: budget.id,
            amount: budget.amount,
            startDate: budget.startDate,
            endDate: budget.endDate,
            categoryId: budget.categoryId,
            categoryName: category ? category.name : 'Unknown'
          } as BudgetView;
        });
      })
    );
  }
}
