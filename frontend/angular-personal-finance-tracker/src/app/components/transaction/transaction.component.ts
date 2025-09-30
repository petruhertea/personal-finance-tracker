import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { Transaction } from '../../common/transaction';
import { TransactionService } from '../../services/transaction.service';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { switchMap } from 'rxjs';
import { UserResponse } from '../../common/stored-user';
import { CategoryService } from '../../services/category.service';
import { Category } from '../../common/category';

@Component({
  selector: 'app-transactions',
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './transaction.component.html',
  styleUrl: './transaction.component.css'
})
export class TransactionsComponent implements OnInit {
  currentUser!: UserResponse; // adaugă asta
  transactions: Transaction[] = [];
  filterForm!: FormGroup;
  transactionForm!: FormGroup;
  editingTransaction: Transaction | null = null;
  categories: Category[] = [];

  constructor(private fb: FormBuilder, private userService: UserService,
    private authService: AuthService, private categoryService: CategoryService,
    private transactionService: TransactionService) { }

  ngOnInit(): void {
    // fetch the user first, after the user is fetched 
    // we load the filter form and categories
    this.authService.getUser().subscribe({
      next: user => {
        if (!user) throw new Error('User not authenticated');
        this.currentUser = user;
        this.categoryService.getCategories(this.currentUser.id).subscribe({
          next: data => {
            this.categories = data;
          },
          error: err => console.error('Error fetching categories', err)
        });

        this.initFilterForm();
        this.initTransactionForm();
        this.loadTransactions();
      },
      error: err => console.error('Error fetching user', err)
    });

  }

  initFilterForm() {
    this.filterForm = this.fb.group({
      type: [''],
      categoryId: [''],
      fromDate: [''],
      toDate: [''],
      minAmount: [''],
      maxAmount: ['']
    });

    // reload list on filter changes
    this.filterForm.valueChanges.subscribe(() => this.loadTransactions());
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
    const filters = this.filterForm.value;
    this.transactionService.getTransactions(this.currentUser.id, filters).subscribe({
      next: (txs: Transaction[]) => this.transactions = txs,
      error: err => console.error('Error loading transactions', err)
    });
  }

  startEdit(tx: Transaction) {
    this.editingTransaction = tx;
    const dateTime = tx.date.toISOString().substring(0, 16); // "2025-09-24"
    this.transactionForm.patchValue({
      amount: tx.amount,
      type: tx.type,
      description: tx.description,
      categoryId: tx.categoryId,
      date: tx.date ? tx.date.toISOString().substring(0, 10) : '' // ia doar yyyy-MM-dd
    });

  }


  cancelEdit() {
    this.editingTransaction = null;
    this.transactionForm.reset();
  }

  saveTransaction() {
    const txData: Transaction = {
      ...this.transactionForm.value,
      id: this.editingTransaction?.id,
      userId: this.currentUser.id,
      date: this.transactionForm.value.date
        ? new Date(this.transactionForm.value.date)
        : undefined
    };



    if (this.editingTransaction) {
      this.transactionService.updateTransaction(txData).subscribe({
        next: () => {
          this.loadTransactions();
          this.cancelEdit();
        },
        error: (err) => console.error('Update failed', err)
      });
    } else {
      this.transactionService.createTransaction(txData).subscribe({
        next: () => {
          this.loadTransactions();
          this.transactionForm.reset();
        },
        error: (err) => console.error('Add failed', err)
      });
    }
  }

  deleteTransaction(id: number) {
    if (confirm('Are you sure you want to delete this transaction?')) {
      this.transactionService.deleteTransaction(id).subscribe({
        next: () => this.loadTransactions(),
        error: (err) => console.error('Delete failed', err)
      });
    }
  }

}
