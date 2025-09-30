import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LineChartComponent } from "../line-chart/line-chart.component";
import { UserResponse } from '../../common/stored-user';
import { UserService } from '../../services/user.service';
import { PieChartComponent } from "../pie-chart/pie-chart.component";

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, LineChartComponent, PieChartComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {
  user?: UserResponse | null;

  constructor(private userService: UserService) { }

  summary: { income: number, expense: number, balance: number } | null = null;

  today = new Date();

  ngOnInit(): void {
    this.userService.getChart().subscribe({
      next: (data) => {
        this.summary = this.userService.getTotals(data);
      },
      error: (err) => console.error(err)
    });
  }
}
