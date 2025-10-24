// src/app/services/transaction.service.ts
import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Transaction } from "../common/transaction";
import { Observable } from "rxjs";
import { environment } from "../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  private apiUrl = `${environment.apiUrl}/transactions`;
  private userUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) { }

  getTransactions(userId: number, filters?: any): Observable<Transaction[]> {
    // Use HttpParams for better parameter handling
    let params = new HttpParams();
    
    if (filters) {
      // Add each filter if it exists and is not empty
      if (filters.type && filters.type !== '') {
        params = params.set('type', filters.type);
        console.log('Added type param:', filters.type);
      }
      if (filters.categoryId && filters.categoryId !== '' && filters.categoryId !== null) {
        params = params.set('categoryId', filters.categoryId.toString());
        console.log('Added categoryId param:', filters.categoryId);
      }
      if (filters.fromDate && filters.fromDate !== '') {
        params = params.set('fromDate', filters.fromDate);
        console.log('Added fromDate param:', filters.fromDate);
      }
      if (filters.toDate && filters.toDate !== '') {
        params = params.set('toDate', filters.toDate);
        console.log('Added toDate param:', filters.toDate);
      }
      if (filters.minAmount && filters.minAmount !== '' && filters.minAmount !== null) {
        params = params.set('minAmount', filters.minAmount.toString());
        console.log('Added minAmount param:', filters.minAmount);
      }
      if (filters.maxAmount && filters.maxAmount !== '' && filters.maxAmount !== null) {
        params = params.set('maxAmount', filters.maxAmount.toString());
        console.log('Added maxAmount param:', filters.maxAmount);
      }
    }

    console.log('Final URL params:', params.toString());
    console.log('Full URL:', `${this.userUrl}/${userId}/transactions?${params.toString()}`);
    
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