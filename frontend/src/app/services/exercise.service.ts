import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

export interface Exercise {
  id: number;
  question: string;
  type: string;
  options: {
    id: string;
    text: string;
  }[];
  correctAnswer?: string;
  explanation: string;
  difficulty: number;
  category: string;
  xpReward: number;
}

export interface ExerciseSubmission {
  exerciseId: number;
  answer: string;
  timeSpent?: number; // em segundos
}

export interface ExerciseResponse {
  correct: boolean;
  explanation: string;
  xpEarned?: number;
  userXp?: number;
  userLevel?: number;
  userStreak?: number;
  correctAnswer: string;
}

export interface ExerciseHistory {
  id: number;
  question: string;
  category: string;
  correct: boolean;
  timestamp: string;
  xpEarned: number;
  /** Verdadeiro quando o item representa uma sessão (ENEM/Construtor), não uma questão individual. */
  isSession?: boolean;
  correctAnswers?: number;
  totalQuestions?: number;
}

export interface ExerciseStats {
  totalExercises: number;
  correctAnswers: number;
  incorrectAnswers: number;
  accuracyRate: number;
  totalXpEarned: number;
  averageTimePerExercise: number;
  categoryStats: Record<string, {
    total: number;
    correct: number;
    accuracy: number;
    averageTime: number;
  }>;
}

@Injectable({
  providedIn: 'root'
})
export class ExerciseService {
  private apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  getExercisesByTrail(trailId: number): Observable<Exercise[]> {
    return this.http.get<Exercise[]>(`${this.apiUrl}/exercises/by-trail/${trailId}`, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      catchError(error => {
        console.error(`Error fetching exercises for trail ${trailId}:`, error);
        return throwError(() => error);
      })
    );
  }

  getExercisesByCategory(category: string, limit: number = 10): Observable<Exercise[]> {
    return this.http.get<Exercise[]>(`${this.apiUrl}/exercises/by-category/${category}`, {
      headers: this.authService.getAuthHeaders(),
      params: { limit: limit.toString() }
    }).pipe(
      catchError(error => {
        console.error(`Error fetching exercises for category ${category}:`, error);
        return throwError(() => error);
      })
    );
  }

  getRandomExercises(count: number = 5): Observable<Exercise[]> {
    return this.http.get<Exercise[]>(`${this.apiUrl}/exercises/random`, {
      headers: this.authService.getAuthHeaders(),
      params: { count: count.toString() }
    }).pipe(
      catchError(error => {
        console.error('Error fetching random exercises:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Questões do ENEM (API pública enem.dev, servida pelo backend).
   * `discipline`: matematica | ciencias-natureza | ciencias-humanas | linguagens.
   */
  getEnemQuestions(discipline: string, limit: number = 10): Observable<Exercise[]> {
    return this.http.get<Exercise[]>(`${this.apiUrl}/exercises/enem/${discipline}`, {
      headers: this.authService.getAuthHeaders(),
      params: { limit: limit.toString() }
    }).pipe(
      catchError(error => {
        console.error(`Error fetching ENEM questions for ${discipline}:`, error);
        return throwError(() => error);
      })
    );
  }

  submitExercise(submission: ExerciseSubmission): Observable<ExerciseResponse> {
    return this.http.post<ExerciseResponse>(`${this.apiUrl}/exercises/submit`, submission, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      map(response => {
        // 0 é um valor válido para streak/XP/level; usar `!= null` evita descartar
        // a sincronização quando o usuário quebrou o streak (userStreak === 0).
        if (response.userXp != null && response.userLevel != null && response.userStreak != null) {
          this.authService.updateUserStats(
            response.userXp,
            response.userLevel,
            response.userStreak
          );
        }
        return response;
      }),
      catchError(error => {
        console.error('Error submitting exercise:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Persiste o XP de uma sessão de quiz composta por questões simuladas/ENEM,
   * que não passam por /submit individual. Também registra a sessão no backend
   * para que ela apareça no painel de desempenho.
   */
  completeSession(
    xp: number,
    correctAnswers: number,
    totalQuestions: number,
    category: string,
    durationSeconds: number = 0
  ): Observable<{ xpEarned: number; userXp: number; userLevel: number; userStreak: number }> {
    return this.http.post<{ xpEarned: number; userXp: number; userLevel: number; userStreak: number }>(
      `${this.apiUrl}/exercises/complete-session`,
      { xp, correctAnswers, totalQuestions, category, durationSeconds },
      { headers: this.authService.getAuthHeaders() }
    ).pipe(
      map(response => {
        if (response.userXp != null && response.userLevel != null && response.userStreak != null) {
          this.authService.updateUserStats(response.userXp, response.userLevel, response.userStreak);
        }
        return response;
      }),
      catchError(error => {
        console.error('Error completing session:', error);
        return throwError(() => error);
      })
    );
  }

  getExerciseHistory(): Observable<ExerciseHistory[]> {
    return this.http.get<ExerciseHistory[]>(`${this.apiUrl}/exercises/history`, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      catchError(error => {
        console.error('Error fetching exercise history:', error);
        return throwError(() => error);
      })
    );
  }

  getExerciseStats(): Observable<ExerciseStats> {
    return this.http.get<ExerciseStats>(`${this.apiUrl}/exercises/stats`, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      catchError(error => {
        console.error('Error fetching exercise stats:', error);
        return throwError(() => error);
      })
    );
  }

  getMockExercises(): Exercise[] {
    return [
      {
        id: 1,
        question: 'Qual é a derivada de f(x) = 3x² + 2x + 1?',
        type: 'multiple-choice',
        options: [
          { id: 'A', text: '6x + 2' },
          { id: 'B', text: '3x + 2' },
          { id: 'C', text: '6x² + 2' },
          { id: 'D', text: '3x² + 2' }
        ],
        correctAnswer: 'A',
        explanation: 'A derivada de x² é 2x, então 3 * 2x = 6x. A derivada de 2x é 2. A derivada de 1 é 0.',
        difficulty: 2,
        category: 'Matemática',
        xpReward: 10
      },
      {
        id: 2,
        question: 'Qual destas palavras é um substantivo?',
        type: 'multiple-choice',
        options: [
          { id: 'A', text: 'Correr' },
          { id: 'B', text: 'Bonito' },
          { id: 'C', text: 'Casa' },
          { id: 'D', text: 'Rapidamente' }
        ],
        correctAnswer: 'C',
        explanation: 'Substantivo é a palavra que nomeia seres, objetos, lugares, sentimentos, etc. "Casa" é um substantivo comum.',
        difficulty: 1,
        category: 'Português',
        xpReward: 10
      }
    ];
  }

  getCategoryIcon(category: string): string {
    const icons: Record<string, string> = {
      'Matemática': 'calculate',
      'Português': 'edit_note',
      'Física': 'science',
      'Química': 'biotech',
      'História': 'history',
      'Geografia': 'public',
      'Biologia': 'psychology',
      'Inglês': 'translate'
    };
    
    return icons[category] || 'question_mark';
  }

  getDifficultyLabel(difficulty: number): string {
    switch (difficulty) {
      case 1: return 'Fácil';
      case 2: return 'Intermediário';
      case 3: return 'Difícil';
      case 4: return 'Avançado';
      case 5: return 'Expert';
      default: return 'Fácil';
    }
  }

  getDifficultyColor(difficulty: number): string {
    switch (difficulty) {
      case 1: return '#4caf50'; // Verde
      case 2: return '#ff9800'; // Laranja
      case 3: return '#f44336'; // Vermelho
      case 4: return '#673ab7'; // Roxo
      case 5: return '#000000'; // Preto
      default: return '#4caf50';
    }
  }
}