export interface BudgetView {
  id: number;
  amount: number;
  // for update/delete
  categoryId: number;
  // for display
  categoryName: string;
  startDate?: Date | undefined;
  endDate?: Date | undefined;
}
