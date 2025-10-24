import { Component, OnInit, OnDestroy, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { UserService } from '../../services/user.service';
import { Transaction } from '../../common/transaction';

Chart.register(...registerables);

@Component({
  selector: 'app-pie-chart',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pie-chart.component.html',
  styleUrls: ['./pie-chart.component.css']
})
export class PieChartComponent implements OnInit, OnDestroy {
  @ViewChild('pieChartCanvas', { static: true }) canvasRef!: ElementRef<HTMLCanvasElement>;

  chart: Chart | undefined;
  summary: { income: number, expense: number, balance: number } | null = null;
  categoryBreakdown: { category: string; amount: number; percentage: number }[] = [];
  transactions: Transaction[] = []; // Store all transactions
  selectedView: 'summary' | 'categories' = 'summary';
  selectedType: 'INCOME' | 'EXPENSE' = 'EXPENSE';
  isLoading = false;

  constructor(
    private userService: UserService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
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
        this.transactions = data; // Store transactions
        this.summary = this.userService.getCurrentMonthSummary(data);
        this.calculateCategoryBreakdown(); // Calculate based on current selectedType
        this.isLoading = false;
        this.renderChart();
      },
      error: (err) => {
        console.error('Error loading chart data:', err);
        this.isLoading = false;
      }
    });
  }

  onViewChange() {
    console.log('View changed to:', this.selectedView); // Debug
    this.renderChart();
  }

  onTypeChange() {
    console.log('Type changed to:', this.selectedType); // Debug
    if (this.selectedView === 'categories') {
      this.calculateCategoryBreakdown(); // Recalculate for new type
      this.renderChart();
    }
  }

  calculateCategoryBreakdown() {
    const now = new Date();
    const currentMonth = now.getMonth();
    const currentYear = now.getFullYear();

    console.log('Calculating breakdown for:', this.selectedType); // Debug

    // Filter current month transactions
    const monthlyTx = this.transactions.filter(tx => {
      const date = new Date(tx.date);
      return date.getMonth() === currentMonth && date.getFullYear() === currentYear;
    });

    console.log('Monthly transactions:', monthlyTx.length); // Debug

    // Group by category for selected type
    const categoryTotals: { [key: string]: number } = {};
    let totalAmount = 0;

    monthlyTx.forEach(tx => {
      if (tx.type === this.selectedType) {
        const category = tx.categoryName || 'Uncategorized';
        categoryTotals[category] = (categoryTotals[category] || 0) + tx.amount;
        totalAmount += tx.amount;
      }
    });

    console.log('Category totals:', categoryTotals); // Debug
    console.log('Total amount:', totalAmount); // Debug

    // Convert to array and calculate percentages
    this.categoryBreakdown = Object.entries(categoryTotals)
      .map(([category, amount]) => ({
        category,
        amount,
        percentage: totalAmount > 0 ? (amount / totalAmount) * 100 : 0
      }))
      .sort((a, b) => b.amount - a.amount)
      .slice(0, 8); // Top 8 categories

    console.log('Category breakdown:', this.categoryBreakdown); // Debug
  }

  renderChart() {
    if (this.chart) {
      this.chart.destroy();
    }

    const config = this.selectedView === 'summary' 
      ? this.getSummaryChartConfig() 
      : this.getCategoryChartConfig();

    if (config && config.data) {
      this.chart = new Chart(this.canvasRef.nativeElement, config);
      this.cdr.detectChanges(); // Force change detection
    }
  }

  private getSummaryChartConfig(): ChartConfiguration {
    if (!this.summary) {
      console.warn('No summary data available'); // Debug
      return {} as ChartConfiguration;
    }

    return {
      type: 'doughnut',
      data: {
        labels: ['Income', 'Expenses'],
        datasets: [{
          data: [this.summary.income, this.summary.expense],
          backgroundColor: [
            'rgba(16, 185, 129, 0.8)',
            'rgba(239, 68, 68, 0.8)'
          ],
          borderColor: [
            '#10b981',
            '#ef4444'
          ],
          borderWidth: 2,
          hoverOffset: 10
        }]
      },
      options: ({
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: true,
            position: 'bottom',
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
            callbacks: {
              label: (context: { label: string; parsed: any; }) => {
                const label = context.label || '';
                const value = context.parsed;
                const formatted = new Intl.NumberFormat('en-US', {
                  style: 'currency',
                  currency: 'USD'
                }).format(value);
                const total = this.summary!.income + this.summary!.expense;
                const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : '0.0';
                return `${label}: ${formatted} (${percentage}%)`;
              }
            }
          }
        },
        cutout: '60%'
      }) as any
    };
  }

  private getCategoryChartConfig(): ChartConfiguration {
    if (this.categoryBreakdown.length === 0) {
      console.warn('No category breakdown data'); // Debug
      return {
        type: 'doughnut',
        data: {
          labels: ['No Data'],
          datasets: [{
            data: [1],
            backgroundColor: ['#e5e7eb']
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false },
            tooltip: { enabled: false }
          }
        }
      } as ChartConfiguration;
    }

    const colors = [
      '#3b82f6', '#10b981', '#f59e0b', '#ef4444',
      '#8b5cf6', '#ec4899', '#14b8a6', '#f97316'
    ];

    return {
      type: 'doughnut',
      data: {
        labels: this.categoryBreakdown.map(c => c.category),
        datasets: [{
          data: this.categoryBreakdown.map(c => c.amount),
          backgroundColor: colors.map(c => c + 'CC'),
          borderColor: colors,
          borderWidth: 2,
          hoverOffset: 10
        }]
      },
      options: ({
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: true,
            position: window.innerWidth < 768 ? 'bottom' : 'right',
            labels: {
              usePointStyle: true,
              padding: 10,
              font: {
                size: 11
              },
              generateLabels: (chart: { data: any; }) => {
                const data = chart.data;
                if (data.labels && data.datasets.length) {
                  return data.labels.map((label: any, i: string | number) => {
                    const value = data.datasets[0].data[i] as number;
                    const formatted = new Intl.NumberFormat('en-US', {
                      style: 'currency',
                      currency: 'USD',
                      minimumFractionDigits: 0
                    }).format(value);
                    return {
                      text: `${label}: ${formatted}`,
                      fillStyle: data.datasets[0].backgroundColor?.[i] as string,
                      hidden: false,
                      index: i
                    };
                  });
                }
                return [];
              }
            }
          },
          tooltip: {
            enabled: true,
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            padding: 12,
            callbacks: {
              label: (context: { label: string; parsed: any; dataIndex: string | number; }) => {
                const label = context.label || '';
                const value = context.parsed;
                const formatted = new Intl.NumberFormat('en-US', {
                  style: 'currency',
                  currency: 'USD'
                }).format(value);
                const percentage = this.categoryBreakdown[context.dataIndex as any].percentage.toFixed(1);
                return `${label}: ${formatted} (${percentage}%)`;
              }
            }
          }
        },
        cutout: '50%'
      }) as any
    };
  }
}