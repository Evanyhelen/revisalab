import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FeedbackService {
  private apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  submitFeedback(rating: number, comment: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/feedback`, { rating, comment }, {
      headers: this.authService.getAuthHeaders()
    });
  }
}
