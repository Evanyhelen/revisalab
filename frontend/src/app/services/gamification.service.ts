import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

export interface Achievement {
  id: number;
  code: string;
  name: string;
  description: string;
  icon: string;
  xpReward: number;
  type: string;
  requiredValue: number;
  unlocked: boolean;
  progress: number;
  completed: boolean;
  unlockedAt?: string;
}

export interface Mission {
  id: number;
  code: string;
  title: string;
  description: string;
  type: string;
  xpReward: number;
  requiredValue: number;
  actionType: string;
  progress: number;
  completed: boolean;
  claimable?: boolean;
  available: boolean;
  personal?: boolean;
}

export interface PersonalMissionRequest {
  title: string;
  description?: string;
  type: 'daily' | 'weekly';
  actionType: 'complete_exercises' | 'study_minutes' | 'streak';
  requiredValue: number;
  xpReward: number;
}

export interface LeaderboardEntry {
  position: number;
  id: number;
  name: string;
  xp: number;
  level: number;
  streak: number;
  createdAt: string;
}

export interface UserGamificationStats {
  xp: number;
  level: number;
  streak: number;
  totalAchievements: number;
  completedMissions: number;
  rank: number;
  xpForNextLevel: number;
  xpProgress: number;
  maxStreak: number;
  streakHistory: {
    date: string;
    studied: boolean;
  }[];
}

export interface RankingResponse {
  sortBy: string;
  ranking: LeaderboardEntry[];
  totalUsers: number;
}

@Injectable({
  providedIn: 'root'
})
export class GamificationService {
  private apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  getUserAchievements(): Observable<Achievement[]> {
    return this.http.get<Achievement[]>(`${this.apiUrl}/gamification/achievements`, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      catchError(error => {
        console.error('Error fetching achievements:', error);
        return throwError(() => error);
      })
    );
  }

  getUserMissions(): Observable<Mission[]> {
    return this.http.get<Mission[]>(`${this.apiUrl}/gamification/missions`, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      catchError(error => {
        console.error('Error fetching missions:', error);
        return throwError(() => error);
      })
    );
  }

  completeMission(missionId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/gamification/missions/${missionId}/complete`, {}, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      map(response => {
        if (response.success && response.xpEarned && response.userXp && response.userLevel) {
          this.authService.updateUserStats(
            response.userXp,
            response.userLevel,
            this.authService.getCurrentUser()?.streak || 0
          );
        }
        return response;
      }),
      catchError(error => {
        console.error('Error completing mission:', error);
        return throwError(() => error);
      })
    );
  }

  createPersonalMission(request: PersonalMissionRequest): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/gamification/personal-missions`, request, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      catchError(error => {
        console.error('Error creating personal mission:', error);
        return throwError(() => error);
      })
    );
  }

  deletePersonalMission(missionId: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/gamification/personal-missions/${missionId}`, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      catchError(error => {
        console.error('Error deleting personal mission:', error);
        return throwError(() => error);
      })
    );
  }

  getRanking(sortBy: string = 'xp', limit: number = 10): Observable<RankingResponse> {
    return this.http.get<RankingResponse>(`${this.apiUrl}/gamification/ranking`, {
      headers: this.authService.getAuthHeaders(),
      params: { sortBy, limit: limit.toString() }
    }).pipe(
      catchError(error => {
        console.error('Error fetching ranking:', error);
        return throwError(() => error);
      })
    );
  }

  getUserGamificationStats(): Observable<UserGamificationStats> {
    return this.http.get<UserGamificationStats>(`${this.apiUrl}/gamification/user-stats`, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      catchError(error => {
        console.error('Error fetching gamification stats:', error);
        return throwError(() => error);
      })
    );
  }

  getMissionIcon(actionType: string): string {
    const icons: Record<string, string> = {
      'complete_exercises': 'assignment_turned_in',
      'study_minutes': 'timer',
      'complete_trail': 'school',
      'streak': 'local_fire_department',
      'correct_streak': 'trending_up',
      'review_errors': 'error'
    };
    
    return icons[actionType] || 'flag';
  }

  getMissionTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      'daily': 'Diária',
      'weekly': 'Semanal',
      'monthly': 'Mensal',
      'special': 'Especial'
    };
    
    return labels[type] || type;
  }

  getMissionTypeColor(type: string): string {
    const colors: Record<string, string> = {
      'daily': '#4caf50', // Verde
      'weekly': '#2196f3', // Azul
      'monthly': '#ff9800', // Laranja
      'special': '#9c27b0' // Roxo
    };
    
    return colors[type] || '#757575';
  }

  getAchievementIcon(type: string): string {
    const icons: Record<string, string> = {
      'streak': 'local_fire_department',
      'level': 'emoji_events',
      'exercise': 'assignment_turned_in',
      'trail': 'school',
      'custom': 'star'
    };
    
    return icons[type] || 'star';
  }

  getAchievementColor(unlocked: boolean): string {
    return unlocked ? '#4caf50' : '#757575';
  }

  calculateXpProgress(currentXp: number, currentLevel: number): { progress: number; xpForNextLevel: number } {
    const xpInCurrentLevel = currentXp % 1000;
    const xpForNextLevel = 1000 - xpInCurrentLevel;
    const progress = (xpInCurrentLevel / 1000) * 100;
    
    return { progress, xpForNextLevel };
  }

  getMockAchievements(): Achievement[] {
    return [
      {
        id: 1,
        code: 'STREAK_3',
        name: 'Estudante Consistente',
        description: 'Mantenha um streak de 3 dias consecutivos',
        icon: 'local_fire_department',
        xpReward: 50,
        type: 'streak',
        requiredValue: 3,
        unlocked: true,
        progress: 3,
        completed: true,
        unlockedAt: '2026-10-05T10:30:00'
      },
      {
        id: 2,
        code: 'STREAK_7',
        name: 'Estudante Dedicado',
        description: 'Mantenha um streak de 7 dias consecutivos',
        icon: 'emoji_events',
        xpReward: 150,
        type: 'streak',
        requiredValue: 7,
        unlocked: false,
        progress: 3,
        completed: false
      },
      {
        id: 3,
        code: 'LEVEL_5',
        name: 'Mestre em Formação',
        description: 'Alcance o nível 5',
        icon: 'school',
        xpReward: 200,
        type: 'level',
        requiredValue: 5,
        unlocked: false,
        progress: 1,
        completed: false
      }
    ];
  }

  getMockMissions(): Mission[] {
    return [
      {
        id: 1,
        code: 'DAILY_5_EXERCISES',
        title: 'Resolva 5 Exercícios',
        description: 'Complete 5 exercícios em qualquer disciplina',
        type: 'daily',
        xpReward: 25,
        requiredValue: 5,
        actionType: 'complete_exercises',
        progress: 3,
        completed: false,
        available: true
      },
      {
        id: 2,
        code: 'DAILY_30_MINUTES',
        title: 'Estude por 30 Minutos',
        description: 'Dedique 30 minutos ao estudo',
        type: 'daily',
        xpReward: 30,
        requiredValue: 30,
        actionType: 'study_minutes',
        progress: 15,
        completed: false,
        available: true
      }
    ];
  }
}