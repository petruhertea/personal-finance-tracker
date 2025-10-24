import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { Transaction } from '../../common/transaction';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-line-chart',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './line-chart.component.html',
  styleUrl: './line-chart.component.css'
})
export class LineChartComponent implements OnInit, OnDestroy {
  @ViewChild('lineChartCanvas', { static: true }) canvasRef!: ElementRef<HTMLCanvasElement>;
  
  transactionList: Transaction[] = [];
  selectedPeriod: 'weekly' | 'monthly' | 'yearly' = 'monthly';
  selectedType: 'INCOME' | 'EXPENSE' | 'BOTH' = 'BOTH';
  chart: Chart | undefined;
  isLoading = false;
  
  constructor(private userService: UserService) {}

  ngOnInit() {
    this.loadChartData();
  }

  ngOnDestroy() {
    if (this.chart) {
      this.chart.destroy();
    }
  }

  loadChartData() {
    this.isLoading = true;
    this.userService.getChart().subscribe({
      next: (data) => {
        this.transactionList = data;
        this.isLoading = false;
        this.renderChart();
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  onPeriodChange() {
    this.renderChart();
  }

  onTypeChange() {
    this.renderChart();
  }

  renderChart() {
    if (this.chart) {
      this.chart.destroy();
    }

    const datasets = this.prepareDatasets();
    const config = this.getChartConfig(datasets);

    this.chart = new Chart(this.canvasRef.nativeElement, config);
  }

  private prepareDatasets() {
    let aggregated: { label: string; income: number; expense: number }[] = [];

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
    }

    const datasets: any[] = [];

    if (this.selectedType === 'INCOME' || this.selectedType === 'BOTH') {
      datasets.push({
        label: 'Income',
        data: aggregated.map(d => d.income),
        borderColor: '#10b981',
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
        borderWidth: 3,
        fill: true,
        tension: 0.4,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBackgroundColor: '#10b981',
        pointBorderColor: '#fff',
        pointBorderWidth: 2
      });
    }

    if (this.selectedType === 'EXPENSE' || this.selectedType === 'BOTH') {
      datasets.push({
        label: 'Expenses',
        data: aggregated.map(d => d.expense),
        borderColor: '#ef4444',
        backgroundColor: 'rgba(239, 68, 68, 0.1)',
        borderWidth: 3,
        fill: true,
        tension: 0.4,
        pointRadius: 4,
        pointHoverRadius: 6,
        pointBackgroundColor: '#ef4444',
        pointBorderColor: '#fff',
        pointBorderWidth: 2
      });
    }

    // Add net savings line if showing both
    if (this.selectedType === 'BOTH') {
      datasets.push({
        label: 'Net Savings',
        data: aggregated.map(d => d.income - d.expense),
        borderColor: '#8b5cf6',
        backgroundColor: 'rgba(139, 92, 246, 0.1)',
        borderWidth: 2,
        borderDash: [5, 5],
        fill: false,
        tension: 0.4,
        pointRadius: 3,
        pointHoverRadius: 5,
        pointBackgroundColor: '#8b5cf6',
        pointBorderColor: '#fff',
        pointBorderWidth: 2
      });
    }

    return {
      labels: aggregated.map(d => d.label),
      datasets: datasets
    };
  }

  private getChartConfig(data: any): ChartConfiguration {
    return {
      type: 'line',
      data: data,
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
          mode: 'index',
          intersect: false,
        },
        plugins: {
          legend: {
            display: true,
            position: 'top',
            labels: {
              usePointStyle: true,
              padding: 15,
              font: {
                size: 12,
                weight: 'bold'
              }
            }
          },
          tooltip: {
            enabled: true,
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            padding: 12,
            titleFont: {
              size: 14,
              weight: 'bold'
            },
            bodyFont: {
              size: 13
            },
            callbacks: {
              label: (context) => {
                let label = context.dataset.label || '';
                if (label) {
                  label += ': ';
                }
                if (context.parsed.y !== null) {
                  label += new Intl.NumberFormat('en-US', {
                    style: 'currency',
                    currency: 'USD'
                  }).format(context.parsed.y);
                }
                return label;
              }
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: (value) => {
                return new Intl.NumberFormat('en-US', {
                  style: 'currency',
                  currency: 'USD',
                  minimumFractionDigits: 0
                }).format(value as number);
              },
              font: {
                size: 11
              }
            },
            grid: {
              color: 'rgba(0, 0, 0, 0.05)',
            }
          },
          x: {
            ticks: {
              font: {
                size: 11
              },
              maxRotation: 45,
              minRotation: 0
            },
            grid: {
              display: false
            }
          }
        }
      }
    };
  }

  private getWeeklyTotals(transactions: Transaction[]) {
    const weekly: { [key: string]: { income: number; expense: number } } = {};
    
    for (let tx of transactions) {
      const date = new Date(tx.date);
      const start = new Date(date);
      start.setDate(date.getDate() - (date.getDay() === 0 ? 6 : date.getDay() - 1));
      
      const end = new Date(start);
      end.setDate(start.getDate() + 6);
      
      const key = `${start.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })} - ${end.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}`;
      
      if (!weekly[key]) {
        weekly[key] = { income: 0, expense: 0 };
      }
      
      if (tx.type === 'INCOME') {
        weekly[key].income += tx.amount;
      } else {
        weekly[key].expense += tx.amount;
      }
    }
    
    return Object.entries(weekly)
      .map(([label, totals]) => ({ label, ...totals }))
      .sort((a, b) => new Date(a.label.split(' - ')[0]).getTime() - new Date(b.label.split(' - ')[0]).getTime());
  }

  private getMonthlyTotals(transactions: Transaction[]) {
    const monthly: { [key: string]: { income: number; expense: number } } = {};
    
    for (let tx of transactions) {
      const date = new Date(tx.date);
      const key = `${date.toLocaleDateString('en-US', { month: 'short', year: 'numeric' })}`;
      
      if (!monthly[key]) {
        monthly[key] = { income: 0, expense: 0 };
      }
      
      if (tx.type === 'INCOME') {
        monthly[key].income += tx.amount;
      } else {
        monthly[key].expense += tx.amount;
      }
    }
    
    return Object.entries(monthly)
      .map(([label, totals]) => ({ label, ...totals }))
      .sort((a, b) => {
        const dateA = new Date(a.label);
        const dateB = new Date(b.label);
        return dateA.getTime() - dateB.getTime();
      });
  }

  private getYearlyTotals(transactions: Transaction[]) {
    const yearly: { [key: string]: { income: number; expense: number } } = {};
    
    for (let tx of transactions) {
      const date = new Date(tx.date);
      const key = `${date.getFullYear()}`;
      
      if (!yearly[key]) {
        yearly[key] = { income: 0, expense: 0 };
      }
      
      if (tx.type === 'INCOME') {
        yearly[key].income += tx.amount;
      } else {
        yearly[key].expense += tx.amount;
      }
    }
    
    return Object.entries(yearly)
      .map(([label, totals]) => ({ label, ...totals }))
      .sort((a, b) => parseInt(a.label) - parseInt(b.label));
  }
}