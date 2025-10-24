import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { BudgetDto } from '../../common/budget-dto';
import { BudgetView } from '../../common/budget-view';
import { BudgetService } from '../../services/budget.service';
import { AuthService } from '../../services/auth.service';
import { UserResponse } from '../../common/user-response';
import { Category } from '../../common/category';
import { CommonModule } from '@angular/common';
import { CategoryService } from '../../services/category.service';

@Component({
  selector: 'app-budget',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './budget.component.html',
  styleUrl: './budget.component.css'
})
export class BudgetComponent implements OnInit {

  budgetList: BudgetView[] = [];
  categories: Category[] = [];
  currentUser!: UserResponse;

  budgetForm!: FormGroup;
  editingBudget: BudgetView | null = null;
  showForm = false; // Add this property to control modal visibility

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
        this.loadBudgets();
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

  private loadBudgets() {
    this.budgetService.getBudgetsWithCategories(this.currentUser.id).subscribe({
      next: budgets => this.budgetList = budgets,
      error: err => console.error('Error loading budgets', err)
    });
  }

  editBudget(budget: BudgetView) {
    this.editingBudget = budget;
    this.showForm = true; // Show the modal

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

  private setFormValues(budget: BudgetView) {
    this.budgetForm.setValue({
      amount: budget.amount,
      categoryId: budget.categoryId,
      startDate: new Date(budget.startDate!).toISOString().substring(0, 10),
      endDate: new Date(budget.endDate!).toISOString().substring(0, 10)
    });
  }

  cancelEdit() {
    this.editingBudget = null;
    this.showForm = false; // Hide the modal
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
      // Update existing budget
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
          this.loadBudgets();
          this.cancelEdit();
        },
        error: err => console.error('Error updating budget', err)
      });

    } else {
      // Create new budget
      const budgetDto: BudgetDto = {
        amount: formValue.amount,
        userId: this.currentUser.id,
        categoryId: formValue.categoryId,
        startDate: new Date(formValue.startDate),
        endDate: new Date(formValue.endDate)
      };

      this.budgetService.createBudget(budgetDto).subscribe({
        next: () => {
          this.loadBudgets();
          this.cancelEdit();
        },
        error: err => console.error('Error creating budget', err)
      });
    }
  }

  deleteBudget(id: number) {
    if (!confirm('Are you sure you want to delete this budget?')) return;
    
    this.budgetService.deleteBudget(id).subscribe({
      next: () => this.loadBudgets(),
      error: err => console.error('Error deleting budget', err)
    });
  }
}