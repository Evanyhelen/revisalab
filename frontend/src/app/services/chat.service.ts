import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

export interface ChatRequest {
  question: string;
}

export interface ChatResponse {
  success: boolean;
  question: string;
  answer: string;
  xpEarned: number;
  userXp: number;
  userLevel: number;
  responseTime: number;
  timestamp: number;
}

export interface ChatHistoryItem {
  role: 'user' | 'ai';
  content: string;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  sendMessage(question: string): Observable<ChatResponse> {
    const headers = this.authService.getAuthHeaders();
    const request: ChatRequest = { question };

    return this.http.post<ChatResponse>(`${this.apiUrl}/chat/message`, request, { headers })
      .pipe(
        catchError(error => {
          console.error('Chat error:', error);
          return throwError(() => error);
        })
      );
  }

  getHistory(): Observable<ChatHistoryItem[]> {
    const headers = this.authService.getAuthHeaders();
    return this.http.get<ChatHistoryItem[]>(`${this.apiUrl}/chat/history`, { headers })
      .pipe(
        catchError(error => {
          console.error('Chat history error:', error);
          return throwError(() => error);
        })
      );
  }

  clearHistory(): Observable<{ success: boolean }> {
    const headers = this.authService.getAuthHeaders();
    return this.http.delete<{ success: boolean }>(`${this.apiUrl}/chat/history`, { headers })
      .pipe(
        catchError(error => {
          console.error('Chat clear error:', error);
          return throwError(() => error);
        })
      );
  }
}