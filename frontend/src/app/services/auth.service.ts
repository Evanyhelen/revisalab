import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';

export interface User {
  id: number;
  name: string;
  email: string;
  xp: number;
  level: number;
  streak: number;
  roles: string[];
  curso?: string;
  semestre?: string;
  instituicao?: string;
  avatar?: string;
}

export interface LoginResponse {
  token: string;
  type: string;
  id: number;
  name: string;
  email: string;
  roles: string[];
  xp: number;
  level: number;
  streak: number;
  curso?: string;
  semestre?: string;
  instituicao?: string;
}

export interface ProfileUpdateRequest {
  name?: string;
  curso?: string;
  semestre?: string;
  instituicao?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  name: string;
  email: string;
  password: string;
  curso?: string;
  semestre?: string;
  instituicao?: string;
}

export interface PasswordRecoveryResponse {
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.apiUrl;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  private tokenKey = 'revisalab_token';
  private userKey = 'revisalab_user';
  private static readonly AVATAR_KEY = 'revisalab_avatar';

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.loadStoredUser();
  }

  private loadStoredUser(): void {
    const token = localStorage.getItem(this.tokenKey);
    const userStr = localStorage.getItem(this.userKey);

    if (token && userStr) {
      try {
        this.currentUserSubject.next(JSON.parse(userStr));
      } catch (error) {
        console.error('Erro ao restaurar usuário salvo:', error);
        this.clearStorage();
      }
    }
  }

  login(loginRequest: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/signin`, loginRequest)
      .pipe(
        tap(response => this.handleAuthentication(response)),
        catchError(error => throwError(() => error))
      );
  }

  signup(signupRequest: SignupRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/auth/signup`, signupRequest)
      .pipe(
        catchError(error => throwError(() => error))
      );
  }

  checkEmail(email: string): Observable<boolean> {
    return this.http.get<{ available: boolean }>(`${this.apiUrl}/auth/check-email`, {
      params: { email }
    }).pipe(
      map(response => response.available),
      catchError(error => throwError(() => error))
    );
  }

  /**
   * Solicita a recuperação de senha. O backend responde sempre da mesma forma,
   * exista ou não a conta, para não revelar se o e-mail está cadastrado.
   */
  forgotPassword(email: string): Observable<PasswordRecoveryResponse> {
    return this.http.post<PasswordRecoveryResponse>(`${this.apiUrl}/auth/forgot-password`, { email })
      .pipe(catchError(error => throwError(() => error)));
  }

  /**
   * Define uma nova senha a partir do token enviado por e-mail.
   */
  resetPassword(token: string, newPassword: string): Observable<PasswordRecoveryResponse> {
    return this.http.post<PasswordRecoveryResponse>(`${this.apiUrl}/auth/reset-password`, {
      token,
      newPassword
    }).pipe(catchError(error => throwError(() => error)));
  }

  private handleAuthentication(response: LoginResponse): void {
    localStorage.setItem(this.tokenKey, response.token);

    const user: User = {
      id: response.id,
      name: response.name,
      email: response.email,
      xp: response.xp,
      level: response.level,
      streak: response.streak,
      roles: response.roles ?? [],
      curso: response.curso,
      semestre: response.semestre,
      instituicao: response.instituicao,
      avatar: this.getAvatar() ?? undefined
    };

    localStorage.setItem(this.userKey, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  logout(skipNavigation: boolean = false): void {
    this.clearStorage();
    this.currentUserSubject.next(null);
    if (!skipNavigation) {
      this.router.navigate(['/auth/login']);
    }
  }

  private clearStorage(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    // O avatar fica em uma chave global; sem limpar, ele "vaza" para a próxima
    // conta que entrar no mesmo dispositivo (avatar do usuário anterior).
    localStorage.removeItem(AuthService.AVATAR_KEY);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  isAdmin(): boolean {
    const user = this.getCurrentUser();
    return user ? user.roles.includes('ROLE_ADMIN') : false;
  }

  getAuthHeaders(): HttpHeaders {
    const token = this.getToken();
    if (token) {
      return new HttpHeaders({
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      });
    }
    return new HttpHeaders({ 'Content-Type': 'application/json' });
  }

  /**
   * Atualiza as estatísticas do usuário logado com valores absolutos
   * retornados pelo backend (userXp, userLevel, userStreak).
   */
  updateUserStats(xp: number, level: number, streak: number): void {
    const currentUser = this.getCurrentUser();
    if (currentUser) {
      const updatedUser: User = {
        ...currentUser,
        xp,
        level,
        streak
      };
      localStorage.setItem(this.userKey, JSON.stringify(updatedUser));
      this.currentUserSubject.next(updatedUser);
    }
  }

  getUserStats(): { xp: number; level: number; streak: number } {
    const user = this.getCurrentUser();
    return {
      xp: user?.xp ?? 0,
      level: user?.level ?? 1,
      streak: user?.streak ?? 0
    };
  }

  /**
   * Atualiza o perfil do usuário (nome, curso, semestre, instituição) via backend.
   */
  updateProfile(profile: ProfileUpdateRequest): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/users/profile`, profile, {
      headers: this.getAuthHeaders()
    }).pipe(
      map(response => {
        const currentUser = this.getCurrentUser();
        if (currentUser) {
          const updatedUser: User = {
            ...currentUser,
            name: response.name ?? currentUser.name,
            curso: response.curso ?? currentUser.curso,
            semestre: response.semestre ?? currentUser.semestre,
            instituicao: response.instituicao ?? currentUser.instituicao
          };
          localStorage.setItem(this.userKey, JSON.stringify(updatedUser));
          this.currentUserSubject.next(updatedUser);
        }
        return response;
      }),
      catchError(error => throwError(() => error))
    );
  }

  /**
   * Retorna o avatar escolhido pelo usuário, ou null quando ele nunca
   * personalizou (assim cada tela mantém seu ícone padrão).
   */
  getAvatar(): string | null {
    return localStorage.getItem(AuthService.AVATAR_KEY);
  }

  /**
   * Persiste o avatar escolhido e o reflete no usuário logado, para que o
   * header e o perfil fiquem sincronizados.
   */
  updateAvatar(avatar: string): void {
    localStorage.setItem(AuthService.AVATAR_KEY, avatar);
    const currentUser = this.getCurrentUser();
    if (currentUser) {
      const updatedUser: User = { ...currentUser, avatar };
      localStorage.setItem(this.userKey, JSON.stringify(updatedUser));
      this.currentUserSubject.next(updatedUser);
    }
  }
}
