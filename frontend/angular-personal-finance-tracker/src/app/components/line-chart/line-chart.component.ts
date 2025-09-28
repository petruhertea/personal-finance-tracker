import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { Transaction } from '../../common/transaction';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-line-chart',
  imports: [CommonModule, FormsModule],
  templateUrl: './line-chart.component.html',
  styleUrl: './line-chart.component.css'
})
export class LineChartComponent {
  constructor(private userService: UserService) {}

  transactionList: Transaction[] = [];
  selectedPeriod: 'weekly' | 'monthly' | 'yearly' = 'weekly';
  chart: Chart | undefined;

  ngOnInit() {
    this.userService.getChart("INCOME").subscribe({
      next: (data) => {
        this.transactionList = data;
        this.renderChart();
      },
      error: (err) => console.error(err)
    });
  }

  renderChart() {
    if (this.chart) {
      this.chart.destroy();
    }

    let aggregated: { label: string; total: number }[] = [];

    switch (this.selectedPeriod) {
      case 'weekly':
        aggregated = this.getWeeklyTotals(this.transactionList);
        break;
      case 'monthly':
        aggregated = this.getMonthlyTotals(this.transactionList);
        break;
      case 'yearly':
        aggregated = this.getYearlyTotals(this.transactionList);
        break;
      default:
        aggregated = this.getWeeklyTotals(this.transactionList);
        break;
    }

    this.chart = new Chart("linechart", {
      type: 'line',
      data: {
        labels: aggregated.map(d => d.label),
        datasets: [{
          label: `Income (${this.selectedPeriod})`,
          data: aggregated.map(d => d.total),
          borderWidth: 2,
          borderColor: 'lime',
          backgroundColor: 'rgba(67, 255, 67, 0.2)',
          fill: true
        }]
      },
      options: {
        responsive: true,
        scales: {
          y: { beginAtZero: true }
        }
      }
    });
  }

  getWeeklyTotals(transactions: Transaction[]) {
    const weekly: { [key: string]: number } = {};
    for (let tx of transactions) {
      const date = new Date(tx.date);

      const start = new Date(date);
      start.setDate(date.getDate() - (date.getDay() === 0 ? 6 : date.getDay() - 1));

      const end = new Date(start);
      end.setDate(start.getDate() + 6);

      const key = `${start.toLocaleDateString('ro-RO')} - ${end.toLocaleDateString('ro-RO')}`;
      weekly[key] = (weekly[key] || 0) + tx.amount;
    }
    return Object.entries(weekly).map(([label, total]) => ({ label, total }));
  }

  getMonthlyTotals(transactions: Transaction[]) {
    const monthly: { [key: string]: number } = {};
    for (let tx of transactions) {
      const date = new Date(tx.date);
      const key = `${date.getMonth() + 1}/${date.getFullYear()}`;
      monthly[key] = (monthly[key] || 0) + tx.amount;
    }
    return Object.entries(monthly).map(([label, total]) => ({ label, total }));
  }

  getYearlyTotals(transactions: Transaction[]) {
    const yearly: { [key: string]: number } = {};
    for (let tx of transactions) {
      const date = new Date(tx.date);
      const key = `${date.getFullYear()}`;
      yearly[key] = (yearly[key] || 0) + tx.amount;
    }
    return Object.entries(yearly).map(([label, total]) => ({ label, total }));
  }
}
