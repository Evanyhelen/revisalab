import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

export interface DashboardStats {
  xp: number;
  level: number;
  streak: number;
  name: string;
  email: string;
  totalExercises: number;
  completedTrails: number;
  totalStudyTime: number;
  accuracyRate: number;
}

export interface LeaderboardEntry {
  id: number;
  name: string;
  xp: number;
  level: number;
  streak: number;
}

export interface ProgressData {
  trailProgress: Record<string, number>;
  weeklyActivity: {
    day: string;
    exercises: number;
    time: number;
  }[];
  recentAchievements: {
    name: string;
    icon: string;
    date: string;
  }[];
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/dashboard/stats`, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      catchError(error => {
        console.error('Error fetching dashboard stats:', error);
        return throwError(() => error);
      })
    );
  }

  getLeaderboard(): Observable<LeaderboardEntry[]> {
    return this.http.get<LeaderboardEntry[]>(`${this.apiUrl}/dashboard/leaderboard`, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      catchError(error => {
        console.error('Error fetching leaderboard:', error);
        return throwError(() => error);
      })
    );
  }

  getProgressData(): Observable<ProgressData> {
    return this.http.get<ProgressData>(`${this.apiUrl}/dashboard/progress`, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      catchError(error => {
        console.error('Error fetching progress data:', error);
        return throwError(() => error);
      })
    );
  }

  getUserRanking(sortBy: string = 'xp', limit: number = 10): Observable<any> {
    return this.http.get(`${this.apiUrl}/gamification/ranking`, {
      headers: this.authService.getAuthHeaders(),
      params: { sortBy, limit: limit.toString() }
    }).pipe(
      catchError(error => {
        console.error('Error fetching ranking:', error);
        return throwError(() => error);
      })
    );
  }

  getUserGamificationStats(): Observable<any> {
    return this.http.get(`${this.apiUrl}/gamification/user-stats`, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      catchError(error => {
        console.error('Error fetching gamification stats:', error);
        return throwError(() => error);
      })
    );
  }

  getQuickStats() {
    const user = this.authService.getCurrentUser();
    return {
      xp: user?.xp || 0,
      level: user?.level || 1,
      streak: user?.streak || 0,
      name: user?.name || 'Usuário'
    };
  }
}