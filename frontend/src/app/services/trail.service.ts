import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

export interface Trail {
  id: number;
  title: string;
  description: string;
  category: string;
  difficulty: number;
  icon: string;
  color: string;
  totalModules: number;
  totalExercises: number;
  estimatedHours: number;
  progress: number;
  completedExercises: number;
}

export interface TrailDetail extends Trail {
  createdAt: string;
  updatedAt: string;
  modules: {
    id: number;
    title: string;
    description: string;
    orderNumber: number;
    totalExercises: number;
    estimatedMinutes: number;
    completed: boolean;
  }[];
  currentModuleOrder?: number;
  currentModuleTitle?: string;
}

export interface TrailCategory {
  name: string;
  count: number;
}

export interface StudyArticle {
  title: string;
  description: string | null;
  extract: string | null;
  thumbnail: { source: string; width: number; height: number } | null;
  url: string | null;
}

export interface StudyVideo {
  title?: string;
  author?: string;
  thumbnail?: string;
  watchUrl: string;
  embedUrl?: string | null;
  searchOnly?: boolean;
}

export interface StudyExtras {
  title: string;
  items: string[];
}

export interface StudyResources {
  article: StudyArticle | null;
  video: StudyVideo | null;
  extras: StudyExtras | null;
}

@Injectable({
  providedIn: 'root'
})
export class TrailService {
  private apiUrl = environment.apiUrl;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  getAllTrails(): Observable<Trail[]> {
    return this.http.get<Trail[]>(`${this.apiUrl}/trails`, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      catchError(error => {
        console.error('Error fetching trails:', error);
        return throwError(() => error);
      })
    );
  }

  getTrailById(id: number): Observable<TrailDetail> {
    return this.http.get<TrailDetail>(`${this.apiUrl}/trails/${id}`, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      catchError(error => {
        console.error(`Error fetching trail ${id}:`, error);
        return throwError(() => error);
      })
    );
  }

  getTrailsByCategory(category: string): Observable<Trail[]> {
    return this.http.get<Trail[]>(`${this.apiUrl}/trails/category/${category}`, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      catchError(error => {
        console.error(`Error fetching trails for category ${category}:`, error);
        return throwError(() => error);
      })
    );
  }

  getTrailCategories(): Observable<TrailCategory[]> {
    return this.http.get<Record<string, number>>(`${this.apiUrl}/trails/categories`, {
      headers: this.authService.getAuthHeaders()
    }).pipe(
      map(categories => {
        return Object.entries(categories).map(([name, count]) => ({
          name,
          count
        }));
      }),
      catchError(error => {
        console.error('Error fetching trail categories:', error);
        return throwError(() => error);
      })
    );
  }

  getStudyResources(category: string, title: string): Observable<StudyResources> {
    return this.http.get<StudyResources>(`${this.apiUrl}/trails/study-resources`, {
      headers: this.authService.getAuthHeaders(),
      params: { category, title }
    }).pipe(
      catchError(error => {
        console.error(`Error fetching study resources for ${category}:`, error);
        return throwError(() => error);
      })
    );
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
      'Inglês': 'translate',
      'Filosofia': 'psychology',
      'Sociologia': 'groups'
    };
    
    return icons[category] || 'menu_book';
  }

  getCategoryColor(category: string): string {
    const colors: Record<string, string> = {
      'Matemática': '#3f51b5',
      'Português': '#f44336',
      'Física': '#4caf50',
      'Química': '#ff9800',
      'História': '#795548',
      'Geografia': '#009688',
      'Biologia': '#673ab7',
      'Inglês': '#2196f3',
      'Filosofia': '#607d8b',
      'Sociologia': '#9c27b0'
    };
    
    return colors[category] || '#757575';
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