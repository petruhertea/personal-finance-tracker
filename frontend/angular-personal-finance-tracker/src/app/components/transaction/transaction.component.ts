import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TransactionService } from '../../services/transaction.service';
import { Transaction } from '../../common/transaction';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { CategoryService } from '../../services/category.service';
import { Category } from '../../common/category';
import { UserResponse } from '../../common/user-response';
import { finalize, debounceTime } from 'rxjs';
import { LoadingSpinnerComponent } from '../loading-spinner/loading-spinner.component';
import { ErrorMessageComponent } from '../error-message/error-message.component';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CommonModule,
    LoadingSpinnerComponent,
    ErrorMessageComponent,
    FormsModule
  ],
  templateUrl: './transaction.component.html',
  styleUrl: './transaction.component.css'
})
export class TransactionComponent implements OnInit {
  currentUser!: UserResponse;
  transactions: Transaction[] = [];
  filterForm!: FormGroup;
  transactionForm!: FormGroup;
  editingTransaction: Transaction | null = null;
  categories: Category[] = [];

  currentPage = 0;
  pageSize = 20;
  totalPages = 0;
  totalElements = 0;

  Math = Math;

  // UI State
  isLoading = false;
  showFilters = false;
  showForm = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private userService: UserService,
    private authService: AuthService,
    private categoryService: CategoryService,
    private transactionService: TransactionService
  ) { }

  ngOnInit(): void {
    this.authService.getUser().subscribe({
      next: user => {
        if (!user) throw new Error('User not authenticated');
        this.currentUser = user;
        this.loadCategories();
        this.initFilterForm();
        this.initTransactionForm();
        this.loadTransactions();
      },
      error: err => {
        this.errorMessage = 'Failed to load user data';
        console.error('Error fetching user', err);
      }
    });
  }

  loadCategories() {
    this.categoryService.getCategories(this.currentUser.id).subscribe({
      next: data => this.categories = data,
      error: err => {
        this.errorMessage = 'Failed to load categories';
        console.error('Error fetching categories', err);
      }
    });
  }

  initFilterForm() {
    // Initialize with null instead of empty strings
    this.filterForm = this.fb.group({
      type: [null],
      categoryId: [null],
      fromDate: [null],
      toDate: [null],
      minAmount: [null],
      maxAmount: [null]
    });

    // Add debounce to prevent too many requests
    this.filterForm.valueChanges
      .pipe(debounceTime(300))
      .subscribe(() => {
        // Reset to first page whenever filters change so backend returns correct page+totals
        this.currentPage = 0;
        console.log('Filter changed:', this.filterForm.value);
        this.loadTransactions();
      });
  }

  initTransactionForm() {
    this.transactionForm = this.fb.group({
      amount: [''],
      type: ['INCOME'],
      description: [''],
      categoryId: [''],
      date: ['']
    });
  }

  loadTransactions() {
    this.isLoading = true;
    const filters = this.buildFilters();

    this.transactionService.getTransactionsPaginated(
      this.currentUser.id,
      filters,
      this.currentPage,
      Number(this.pageSize)
    ).pipe(
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: (response) => {
        this.transactions = response.content;
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
        this.errorMessage = '';
      },
      error: err => {
        this.errorMessage = 'Failed to load transactions';
        console.error('Error loading transactions', err);
      }
    });
  }

  nextPage() {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadTransactions();
    }
  }

  previousPage() {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadTransactions();
    }
  }

  goToPage(page: number) {
    this.currentPage = page;
    this.loadTransactions();
  }

  // Build filters object, excluding null/empty values
  private buildFilters(): any {
    const formValue = this.filterForm.value;
    const filters: any = {};

    // Only include non-null, non-empty values
    if (formValue.type && formValue.type !== '') {
      filters.type = formValue.type;
    }
    if (formValue.categoryId && formValue.categoryId !== '') {
      filters.categoryId = formValue.categoryId;
    }
    if (formValue.fromDate && formValue.fromDate !== '') {
      filters.fromDate = formValue.fromDate;
    }
    if (formValue.toDate && formValue.toDate !== '') {
      filters.toDate = formValue.toDate;
    }
    if (formValue.minAmount !== null && formValue.minAmount !== '') {
      filters.minAmount = formValue.minAmount;
    }
    if (formValue.maxAmount !== null && formValue.maxAmount !== '') {
      filters.maxAmount = formValue.maxAmount;
    }

    return filters;
  }

  startEdit(tx: Transaction) {
    this.editingTransaction = tx;
    this.showForm = true;

    const dateTime = new Date(tx.date!).toISOString().substring(0, 16);
    this.transactionForm.patchValue({
      amount: tx.amount,
      type: tx.type,
      description: tx.description,
      categoryId: tx.categoryId,
      date: dateTime
    });
  }

  cancelEdit() {
    this.editingTransaction = null;
    this.showForm = false;
    this.transactionForm.reset({
      type: 'INCOME'
    });
  }

  saveTransaction() {
    if (this.transactionForm.invalid) return;

    const txData: Transaction = {
      ...this.transactionForm.value,
      id: this.editingTransaction?.id,
      userId: this.currentUser.id,
      date: this.transactionForm.value.date
        ? new Date(this.transactionForm.value.date)
        : undefined
    };

    this.isLoading = true;
    const operation = this.editingTransaction
      ? this.transactionService.updateTransaction(txData)
      : this.transactionService.createTransaction(txData);

    operation.pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: () => {
          this.loadTransactions();
          this.cancelEdit();
          this.errorMessage = '';
        },
        error: (err) => {
          this.errorMessage = this.editingTransaction
            ? 'Failed to update transaction'
            : 'Failed to create transaction';
          console.error('Save failed', err);
        }
      });
  }

  deleteTransaction(id: number) {
    if (!confirm('Are you sure you want to delete this transaction?')) return;

    this.isLoading = true;
    this.transactionService.deleteTransaction(id)
      .pipe(finalize(() => this.isLoading = false))
      .subscribe({
        next: () => {
          this.loadTransactions();
          this.errorMessage = '';
        },
        error: (err) => {
          this.errorMessage = 'Failed to delete transaction';
          console.error('Delete failed', err);
        }
      });
  }

  clearError() {
    this.errorMessage = '';
  }

  resetFilters() {
    this.filterForm.reset({
      type: null,
      categoryId: null,
      fromDate: null,
      toDate: null,
      minAmount: null,
      maxAmount: null
    });
    this.currentPage = 0;
    this.loadTransactions();
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;

    let start = Math.max(0, this.currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(this.totalPages, start + maxVisible);

    if (end - start < maxVisible) {
      start = Math.max(0, end - maxVisible);
    }

    for (let i = start; i < end; i++) {
      pages.push(i);
    }

    return pages;
  }

  onPageSizeChange() {
    this.currentPage = 0; // Reset to first page
    this.pageSize = Number(this.pageSize);
    this.loadTransactions();
  }
}