import { Injectable } from '@angular/core';
import { AuthService } from '../auth.service';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Category } from '../common/category';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {

  private userUrl = 'http://localhost:8080/api/users';

  constructor(private authService: AuthService, private http: HttpClient) { }

  getCategories(userId: number): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.userUrl}/${userId}/categories`);
  }

}
