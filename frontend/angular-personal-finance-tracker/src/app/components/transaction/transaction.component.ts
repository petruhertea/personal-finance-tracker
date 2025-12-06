// transaction.component.ts - FIXED VERSION
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

  // Pagination state
  currentPage = 0; // Backend uses 0-based indexing
  pageSize: number = 20; // ✅ Explicitly typed as number with initial value
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
        // ✅ Reset to first page whenever filters change
        this.currentPage = 0;
        console.log('Filter changed, resetting to page 0');
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

    console.log('🔍 Loading transactions with:', {
      page: this.currentPage,
      size: this.pageSize,
      sizeType: typeof this.pageSize,
      filters: filters
    });

    this.transactionService.getTransactionsPaginated(
      this.currentUser.id,
      filters,
      this.currentPage, // Backend expects 0-based
      Number(this.pageSize) // ✅ Ensure it's always a number
    ).pipe(
      finalize(() => this.isLoading = false)
    ).subscribe({
      next: (response) => {
        console.log('✅ Received response:', {
          content: response.content.length,
          totalPages: response.totalPages,
          totalElements: response.totalElements,
          currentPage: response.number,
          size: response.size
        });

        this.transactions = response.content;
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
        this.currentPage = response.number; // ✅ Update current page from response
        this.errorMessage = '';
      },
      error: err => {
        this.errorMessage = 'Failed to load transactions';
        console.error('❌ Error loading transactions', err);
      }
    });
  }

  nextPage() {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      console.log('Next page:', this.currentPage);
      this.loadTransactions();
    }
  }

  previousPage() {
    if (this.currentPage > 0) {
      this.currentPage--;
      console.log('Previous page:', this.currentPage);
      this.loadTransactions();
    }
  }

  goToPage(page: number) {
    console.log('Go to page:', page);
    this.currentPage = page;
    this.loadTransactions();
  }

  // ✅ Build filters object, excluding null/empty values
  private buildFilters(): any {
    const formValue = this.filterForm.value;
    const filters: any = {};

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

  // ✅ Fixed page numbers display (convert from 0-based to 1-based for UI)
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
    console.log('Page size change triggered:', this.pageSize, typeof this.pageSize);
    
    // ✅ CRITICAL: Force conversion to number AND reset page
    this.pageSize = Number(this.pageSize);
    this.currentPage = 0; // Reset to first page
    
    console.log('After conversion - pageSize:', this.pageSize, 'type:', typeof this.pageSize);
    console.log('Resetting to page 0');
    
    // Force immediate load
    this.loadTransactions();
  }

  // ✅ Helper to get display page number (1-based for UI)
  getDisplayPageNumber(page: number): number {
    return page + 1;
  }

  // ✅ Helper to get start index for display
  getStartIndex(): number {
    return this.currentPage * this.pageSize + 1;
  }

  // ✅ Helper to get end index for display
  getEndIndex(): number {
    return Math.min((this.currentPage + 1) * this.pageSize, this.totalElements);
  }
}