// budget-with-spending.ts (create in src/app/common/)
export interface BudgetWithSpending {
  // Original budget fields
  id?: number;
  categoryId: number;
  categoryName?: string; // For display
  budgetAmount: number;
  userId: number;
  
  startDate?: Date;
  endDate?: Date;
  
  // Spending calculation fields
  spent: number;        // Total spent in this category during period
  percentage: number;      // (spending / amount) * 100
  remaining: number;       // amount - spending
  isOverBudget: boolean;   // percentage > 100
  nearThreshold: boolean; // percentage >= alertThreshold (default 80)
  alertThreshold?: number; // Default 80%
}