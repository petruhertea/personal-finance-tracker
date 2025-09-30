import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { AuthService } from "../services/auth.service";
import { Transaction } from "../common/transaction";
import { Observable } from "rxjs";

// transaction.service.ts
@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  private apiUrl = 'http://localhost:8080/api/transactions';
  private userUrl = 'http://localhost:8080/api/users';

  constructor(private http: HttpClient, private auth: AuthService) { }

  getTransactions(userId: number, filters?: any): Observable<Transaction[]> {
    const params: any = {};
    if (filters) {
      if (filters.type) params.type = filters.type;
      if (filters.categoryId) params.categoryId = filters.categoryId;
      if (filters.fromDate) params.fromDate = filters.fromDate;
      if (filters.toDate) params.toDate = filters.toDate;
      if (filters.minAmount) params.minAmount = filters.minAmount;
      if (filters.maxAmount) params.maxAmount = filters.maxAmount;
    }
    return this.http.get<Transaction[]>(`${this.userUrl}/${userId}/transactions`, { params });
  }


  createTransaction(tx: Transaction): Observable<Transaction> {
    return this.http.post<Transaction>(this.apiUrl, tx);
  }

  updateTransaction(tx: Transaction): Observable<Transaction> {
    return this.http.put<Transaction>(this.apiUrl, tx);
  }

  deleteTransaction(id: number): Observable<string> {
    return this.http.delete(`${this.apiUrl}/${id}`, { responseType: 'text' });
  }
}

