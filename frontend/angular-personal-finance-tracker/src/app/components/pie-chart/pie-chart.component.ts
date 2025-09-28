import { Component, OnInit } from '@angular/core';
import { Chart, registerables } from 'chart.js';
import { UserService } from '../../services/user.service';

Chart.register(...registerables);

@Component({
  selector: 'app-pie-chart',
  templateUrl: './pie-chart.component.html',
  styleUrls: ['./pie-chart.component.css']
})
export class PieChartComponent implements OnInit {

  chart: Chart | undefined;
  summary: { income: number, expense: number, balance: number } | null = null;

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.userService.getChart().subscribe({
      next: (data) => {
        this.summary = this.userService.getCurrentMonthSummary(data);
        this.renderChart(); // <-- important: apelăm aici
      },
      error: (err) => console.error(err)
    });
  }

  renderChart() {
    if (!this.summary) return; // fallback de siguranță

    if (this.chart) {
      this.chart.destroy();
    }

    this.chart = new Chart("piechart", {
      type: 'pie',
      data: {
        labels: ['Income', 'Expense'],
        datasets: [
          {
            data: [this.summary.income, this.summary.expense],
            backgroundColor: ['lime', 'red'],
            borderWidth: 1
          }
        ]
      },
      options: {
        responsive: true,
        plugins: {
          legend: {
            position: 'bottom'
          }
        }
      }
    });
  }
}
