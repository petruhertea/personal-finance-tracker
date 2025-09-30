import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AuthService } from './auth.service';
import { map, Observable, switchMap, tap } from 'rxjs';
import { Transaction } from '../common/transaction';
import { UserResponse } from '../common/stored-user';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private apiUrl = 'http://localhost:8080/api/users'; // backend-ul tău
  private transactionList!: Transaction[];

  constructor(private http: HttpClient, private authService: AuthService) { }

  getChart(type?: string): Observable<Transaction[]> {
    return this.authService.getUser().pipe(
      map((user: UserResponse) => {
        if (!user) {
          throw new Error('User is not authenticated.');
        }
        return user;
      }),
      // switchMap to chain the HTTP request after getting the user
      // Use switchMap so the Observable emits the HTTP result
      // Use type check for the endpoint
      // Use tap to process transactions
      // Use type assertion for Transaction[]
      // Use correct endpoint based on type
      switchMap((user: UserResponse) => {
        if (type == null) {
          return this.http.get<Transaction[]>(`${this.apiUrl}/${user.id}/transactions`).pipe(
            tap((transactions: Transaction[]) => {
              this.transactionList = transactions;
            })
          );
        } else {
          return this.http.get<Transaction[]>(`${this.apiUrl}/${user.id}/transactions/chart?type=${type}`).pipe(
            tap((transactions: Transaction[]) => {
              this.transactionList = transactions;
            })
          );
        }
      })
    );
  }

  getCurrentMonthSummary(transactions: Transaction[]) {
    const now = new Date();
    const currentMonth = now.getMonth();
    const currentYear = now.getFullYear();

    const monthlyTx = transactions.filter(tx => {
      const date = new Date(tx.date);
      return date.getMonth() === currentMonth && date.getFullYear() === currentYear;
    });

    const income = monthlyTx
      .filter(tx => tx.type === 'INCOME')
      .reduce((sum, tx) => sum + tx.amount, 0);

    const expense = monthlyTx
      .filter(tx => tx.type === 'EXPENSE')
      .reduce((sum, tx) => sum + tx.amount, 0);

    return {
      income,
      expense,
      balance: income - expense
    };
  }

  getTotals(transactions: Transaction[]){
    let income = 0;
    let expense = 0;
    for(let transaction of transactions){
      if(transaction.type==="INCOME"){
        income+=transaction.amount;
      }else{
        expense+=transaction.amount;
      }
    }


    return {
      income,
      expense,
      balance: income - expense
    };
  }

}
