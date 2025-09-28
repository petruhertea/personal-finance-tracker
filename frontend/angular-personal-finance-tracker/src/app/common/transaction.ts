export class Transaction {
    id?: number;
    amount!: number;
    type!: string;
    description!: string;
    date!: Date;
    userId?: number;
    categoryId?: number;
    categoryName!: string;
}
