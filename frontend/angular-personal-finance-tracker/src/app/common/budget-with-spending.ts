// budget-with-spending.ts (create in src/app/common/)
export interface BudgetWithSpending {
  // Original budget fields
  id?: number;
  amount: number;
  userId: number;
  categoryId: number;
  categoryName?: string; // For display
  startDate?: Date;
  endDate?: Date;
  
  // Spending calculation fields
  spending: number;        // Total spent in this category during period
  percentage: number;      // (spending / amount) * 100
  remaining: number;       // amount - spending
  isOverBudget: boolean;   // percentage > 100
  isNearThreshold: boolean; // percentage >= alertThreshold (default 80)
  alertThreshold?: number; // Default 80%
}