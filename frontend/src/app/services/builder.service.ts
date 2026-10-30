import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class BuilderService {
  private apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  completeSession(correctAnswers: number, totalQuestions: number, durationSeconds: number = 0): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/builder/complete`, { correctAnswers, totalQuestions, durationSeconds }, {
      headers: this.authService.getAuthHeaders()
    });
  }

  submitAnswer(questionId: number, answer: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/builder/submit`, { questionId, answer }, {
      headers: this.authService.getAuthHeaders()
    });
  }

  getQuestions(limit: number = 8): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/builder/questions`, {
      params: { limit: String(limit) },
      headers: this.authService.getAuthHeaders()
    });
  }
}
