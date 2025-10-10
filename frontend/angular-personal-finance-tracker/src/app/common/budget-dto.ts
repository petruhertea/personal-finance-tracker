export class BudgetDto {
    id?: number | undefined;
    amount!: number;
    userId!: number;
    categoryId!: number;
    startDate?: Date;
    endDate?: Date;
}
