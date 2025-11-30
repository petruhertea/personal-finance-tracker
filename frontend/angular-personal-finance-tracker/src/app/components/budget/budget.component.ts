// budget.component.ts
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { BudgetDto } from '../../common/budget-dto';
import { BudgetService } from '../../services/budget.service';
import { AuthService } from '../../services/auth.service';
import { UserResponse } from '../../common/user-response';
import { Category } from '../../common/category';
import { CommonModule } from '@angular/common';
import { CategoryService } from '../../services/category.service';
import { BudgetWithSpending } from '../../common/budget-with-spending';

@Component({
  selector: 'app-budget',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './budget.component.html',
  styleUrl: './budget.component.css'
})
export class BudgetComponent implements OnInit {

  budgetList: BudgetWithSpending[] = [];
  categories: Category[] = [];
  currentUser!: UserResponse;

  budgetForm!: FormGroup;
  editingBudget: BudgetWithSpending | null = null;
  showForm = false;

  constructor(
    private budgetService: BudgetService,
    private authService: AuthService,
    private fb: FormBuilder,
    private categoryService: CategoryService
  ) { }

  ngOnInit(): void {
    this.authService.getUser().subscribe({
      next: user => {
        if (!user) throw new Error('User not authenticated');
        this.currentUser = user;
        this.loadCategories(this.currentUser.id);
        this.initForm();
        this.loadBudgetsWithSpending(this.currentUser.id);
      }
    });
  }

  loadCategories(userId: number) {
    this.categoryService.getCategories(userId).subscribe({
      next: data => {
        this.categories = data;
        if (this.categories.length > 0 && !this.editingBudget) {
          this.budgetForm.get('categoryId')?.setValue(this.categories[0].id);
        }
      },
      error: err => console.error("Error fetching categories", err)
    });
  }

  private initForm() {
    this.budgetForm = this.fb.group({
      amount: [0, [Validators.required, Validators.min(0.01)]],
      categoryId: [null, Validators.required],
      startDate: [new Date().toISOString().substring(0, 10), Validators.required],
      endDate: [new Date().toISOString().substring(0, 10), Validators.required]
    });
  }

  private loadBudgetsWithSpending(userId: number) {
    this.budgetService.getBudgetsWithSpending(userId).subscribe({
      next: budgets => {
        this.budgetList = budgets;
        this.showBudgetAlerts(budgets);
        console.log("Budget List",this.budgetList);
      },
      error: err => console.error('Error loading budgets with spending', err)
    });
  }

  // ✅ Show in-app alerts for budget warnings
  private showBudgetAlerts(budgets: BudgetWithSpending[]) {
    budgets.forEach(budget => {
      if (budget.isOverBudget) {
        console.warn(`🚨 Budget exceeded for ${budget.categoryName}: ${budget.percentage.toFixed(1)}%`);
      } else if (budget.nearThreshold) {
        console.warn(`⚠️ Budget warning for ${budget.categoryName}: ${budget.percentage.toFixed(1)}%`);
      }
    });
  }

  // ✅ Helper: Get progress bar color based on percentage
  getProgressBarColor(budget: BudgetWithSpending): string {
    if (budget.isOverBudget) return 'bg-danger';
    if (budget.nearThreshold) return 'bg-warning';
    return 'bg-success';
  }

  // ✅ Helper: Get progress bar percentage (capped at 100%)
  getProgressWidth(budget: BudgetWithSpending): string {
    return Math.min(budget.percentage, 100).toFixed(1) + '%';
  }

  // ✅ Helper: Get status badge
  getBudgetStatus(budget: BudgetWithSpending): string {
    if (budget.isOverBudget) return 'Over Budget';
    if (budget.nearThreshold) return 'Warning';
    return 'On Track';
  }

  // ✅ Helper: Get status badge class
  getStatusBadgeClass(budget: BudgetWithSpending): string {
    if (budget.isOverBudget) return 'bg-danger';
    if (budget.nearThreshold) return 'bg-warning';
    return 'bg-success';
  }

  editBudget(budget: BudgetWithSpending) {
    this.editingBudget = budget;
    this.showForm = true;

    if (this.categories.length === 0) {
      this.categoryService.getCategories(this.currentUser.id).subscribe({
        next: cats => {
          this.categories = cats;
          this.setFormValues(budget);
        },
        error: err => console.error("Error fetching categories", err)
      });
    } else {
      this.setFormValues(budget);
    }
  }

  private setFormValues(budget: BudgetWithSpending) {
    this.budgetForm.setValue({
      amount: budget.budgetAmount,
      categoryId: budget.categoryId,
      startDate: new Date(budget.startDate!).toISOString().substring(0, 10),
      endDate: new Date(budget.endDate!).toISOString().substring(0, 10)
    });
  }

  cancelEdit() {
    this.editingBudget = null;
    this.showForm = false;
    this.budgetForm.reset({
      amount: 0,
      categoryId: this.categories.length > 0 ? this.categories[0].id : null,
      startDate: new Date().toISOString().substring(0, 10),
      endDate: new Date().toISOString().substring(0, 10)
    });
  }

  saveBudget() {
    if (this.budgetForm.invalid) return;

    const formValue = this.budgetForm.value;

    if (this.editingBudget) {
      const budgetDto: BudgetDto = {
        id: this.editingBudget.id,
        amount: formValue.amount,
        userId: this.currentUser.id,
        categoryId: formValue.categoryId,
        startDate: new Date(formValue.startDate),
        endDate: new Date(formValue.endDate)
      };

      this.budgetService.updateBudget(budgetDto.id!, budgetDto).subscribe({
        next: () => {
          this.loadBudgetsWithSpending(this.currentUser.id);
          this.cancelEdit();
        },
        error: err => console.error('Error updating budget', err)
      });
    } else {
      const budgetDto: BudgetDto = {
        amount: formValue.amount,
        userId: this.currentUser.id,
        categoryId: formValue.categoryId,
        startDate: new Date(formValue.startDate),
        endDate: new Date(formValue.endDate)
      };

      this.budgetService.createBudget(budgetDto).subscribe({
        next: () => {
          this.loadBudgetsWithSpending(this.currentUser.id);
          this.cancelEdit();
        },
        error: err => console.error('Error creating budget', err)
      });
    }
  }

  deleteBudget(id: number) {
    if (!confirm('Are you sure you want to delete this budget?')) return;

    this.budgetService.deleteBudget(id).subscribe({
      next: () => this.loadBudgetsWithSpending(this.currentUser.id),
      error: err => console.error('Error deleting budget', err)
    });
  }

  hasBudgetAlerts(){
    return this.budgetList.some(b => b.isOverBudget || b.nearThreshold);
  }
}