import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LineChartComponent } from "../line-chart/line-chart.component";
import { PieChartComponent } from "../pie-chart/pie-chart.component";
import { UserResponse } from '../../common/user-response';
import { UserService } from '../../services/user.service';
import { Transaction } from '../../common/transaction';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, LineChartComponent, PieChartComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  user?: UserResponse | null;
  summary: { income: number, expense: number, balance: number } | null = null;
  recentTransactions: Transaction[] = [];
  today = new Date();
  isLoading = false;

  Math = Math;

  // Quick stats
  savingsRate: number = 0;
  topCategory: { name: string; amount: number } | null = null;
  transactionCount: number = 0;

  constructor(private userService: UserService) { }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData() {
    this.isLoading = true;
    this.userService.getChart().subscribe({
      next: (data) => {
        this.summary = this.userService.getTotals(data);
        this.calculateQuickStats(data);
        this.getRecentTransactions(data);
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  calculateQuickStats(transactions: Transaction[]) {
    if (!this.summary) return;

    // Calculate savings rate
    if (this.summary.income > 0) {
      this.savingsRate = (this.summary.balance / this.summary.income) * 100;
    }

    // Find top spending category
    const categoryTotals: { [key: string]: number } = {};
    transactions.forEach(tx => {
      if (tx.type === 'EXPENSE') {
        const category = tx.categoryName || 'Uncategorized';
        categoryTotals[category] = (categoryTotals[category] || 0) + tx.amount;
      }
    });

    const topEntry = Object.entries(categoryTotals)
      .sort((a, b) => b[1] - a[1])[0];

    if (topEntry) {
      this.topCategory = {
        name: topEntry[0],
        amount: topEntry[1]
      };
    }

    this.transactionCount = transactions.length;
  }

  getRecentTransactions(transactions: Transaction[]) {
    this.recentTransactions = transactions
      .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
      .slice(0, 5);
  }

  getSavingsRateClass(): string {
    if (this.savingsRate >= 20) return 'text-success';
    if (this.savingsRate >= 10) return 'text-warning';
    return 'text-danger';
  }

  getBalanceClass(): string {
    if (!this.summary) return '';
    return this.summary.balance >= 0 ? 'text-success' : 'text-danger';
  }
}