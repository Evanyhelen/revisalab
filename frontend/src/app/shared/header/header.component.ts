import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../../services/auth.service';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    MatDividerModule
  ],
  template: `
    <header class="revisa-header" [class.scrolled]="isScrolled" [class.auth-header]="isAuthPage">
      <div class="header-inner">

        <!-- Logo -->
        <a class="logo" [routerLink]="isAuthenticated() ? '/dashboard' : '/'" [style.cursor]="'pointer'">
          <span class="logo-icon">📖</span>
          <span class="logo-text">RevisaLab</span>
        </a>

        <!-- Nav pública (não logado) -->
        <nav class="nav-public" *ngIf="!isAuthenticated() && !isAuthPage">
          <a class="nav-link" [routerLink]="['/']" fragment="como-funciona">Como Funciona</a>
          <a class="nav-link" [routerLink]="['/']" fragment="funcionalidades">Funcionalidades</a>
          <a class="nav-link" [routerLink]="['/']" fragment="depoimentos">Depoimentos</a>
        </nav>

        <!-- Nav logada -->
        <nav class="nav-app" *ngIf="isAuthenticated()">
          <!-- Navegação removida - agora está no sidebar lateral -->
        </nav>

        <!-- Ações direita -->
        <div class="header-actions">
          <!-- Usuário logado -->
          <ng-container *ngIf="isAuthenticated()">
            <div class="xp-badge" matTooltip="Seu XP total">
              <span class="xp-icon">⚡</span>
              <span class="xp-value">{{ userXp }} XP</span>
            </div>
            <div class="level-badge" matTooltip="Seu nível">
              <span class="level-text">Nv. {{ userLevel }}</span>
            </div>
            <button class="avatar-btn" [matMenuTriggerFor]="userMenu" matTooltip="Meu perfil">
              <span class="avatar-emoji">{{ userAvatar }}</span>
            </button>
            <mat-menu #userMenu="matMenu" class="user-menu-panel">
              <a mat-menu-item routerLink="/perfil">
                <mat-icon>person</mat-icon> Meu Perfil
              </a>
              <mat-divider></mat-divider>
              <button mat-menu-item (click)="logout()">
                <mat-icon>logout</mat-icon> Sair
              </button>
            </mat-menu>
          </ng-container>

          <!-- Não logado -->
          <ng-container *ngIf="!isAuthenticated() && !isAuthPage">
            <a class="btn-ghost" routerLink="/auth/login">Login</a>
            <a class="btn-primary" routerLink="/auth/register">Cadastrar</a>
          </ng-container>

          <!-- Mobile menu toggle -->
          <button class="mobile-toggle" (click)="toggleMobileMenu()" *ngIf="isAuthenticated()">
            <mat-icon>{{ mobileMenuOpen ? 'close' : 'menu' }}</mat-icon>
          </button>
        </div>
      </div>

      <!-- Mobile nav logada -->
      <div class="mobile-nav" *ngIf="isAuthenticated() && mobileMenuOpen">
        <!-- Navegação removida - agora está no sidebar lateral -->
        <a class="mobile-nav-item" routerLink="/perfil" (click)="closeMobileMenu()">
          <mat-icon>person</mat-icon> Perfil
        </a>
        <button class="mobile-logout" (click)="logout()">
          <mat-icon>logout</mat-icon> Sair
        </button>
      </div>
    </header>
  `,
  styles: [`
    .revisa-header {
      position: sticky;
      top: 0;
      z-index: 1000;
      background: #1a2980;
      box-shadow: 0 2px 20px rgba(26, 41, 128, 0.3);
      transition: all 0.3s ease;
      font-family: 'Inter', sans-serif;
    }

    .revisa-header.scrolled {
      background: rgba(26, 41, 128, 0.97);
      backdrop-filter: blur(12px);
      box-shadow: 0 4px 30px rgba(26, 41, 128, 0.4);
    }

    .header-inner {
      max-width: 1280px;
      margin: 0 auto;
      padding: 0 24px;
      height: 64px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 24px;
    }

    /* Logo */
    .logo {
      display: flex;
      align-items: center;
      gap: 8px;
      text-decoration: none;
      flex-shrink: 0;
    }

    .logo-icon {
      font-size: 22px;
      line-height: 1;
    }

    .logo-text {
      font-size: 22px;
      font-weight: 800;
      color: white;
      letter-spacing: -0.5px;
    }

    .logo-plus {
      color: #26d0ce;
    }

    /* Nav pública */
    .nav-public {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .nav-link {
      color: rgba(255,255,255,0.85);
      text-decoration: none;
      font-size: 14px;
      font-weight: 500;
      padding: 8px 14px;
      border-radius: 8px;
      transition: all 0.2s ease;

      &:hover {
        color: white;
        background: rgba(255,255,255,0.1);
      }
    }

    /* Nav app */
    .nav-app {
      display: flex;
      align-items: center;
      gap: 2px;
      flex: 1;
      justify-content: center;
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 6px;
      color: rgba(255,255,255,0.8);
      text-decoration: none;
      font-size: 13px;
      font-weight: 500;
      padding: 8px 12px;
      border-radius: 10px;
      transition: all 0.2s ease;

      mat-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
      }

      &:hover {
        color: white;
        background: rgba(255,255,255,0.1);
      }

      &.active {
        color: white;
        background: rgba(38, 208, 206, 0.25);
        font-weight: 600;
      }
    }

    /* Ações direita */
    .header-actions {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-shrink: 0;
    }

    /* XP badge */
    .xp-badge {
      display: flex;
      align-items: center;
      gap: 4px;
      background: rgba(245, 166, 35, 0.2);
      border: 1px solid rgba(245, 166, 35, 0.4);
      border-radius: 20px;
      padding: 4px 12px;
      cursor: default;
    }

    .xp-icon {
      font-size: 14px;
    }

    .xp-value {
      font-size: 12px;
      font-weight: 700;
      color: #f5a623;
    }

    /* Level badge */
    .level-badge {
      background: rgba(38, 208, 206, 0.2);
      border: 1px solid rgba(38, 208, 206, 0.4);
      border-radius: 20px;
      padding: 4px 12px;
      cursor: default;
    }

    .level-text {
      font-size: 12px;
      font-weight: 700;
      color: #26d0ce;
    }

    /* Avatar */
    .avatar-btn {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      border: 2px solid rgba(255,255,255,0.3);
      background: rgba(255,255,255,0.1);
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.2s ease;

      &:hover {
        border-color: #26d0ce;
        background: rgba(38,208,206,0.2);
        transform: scale(1.05);
      }
    }

    .avatar-emoji {
      font-size: 18px;
      line-height: 1;
    }

    /* Botões não logado */
    .btn-ghost {
      color: white;
      text-decoration: none;
      font-size: 14px;
      font-weight: 500;
      padding: 8px 16px;
      border-radius: 8px;
      border: 1px solid rgba(255,255,255,0.3);
      transition: all 0.2s ease;

      &:hover {
        background: rgba(255,255,255,0.1);
        border-color: white;
      }
    }

    .btn-primary {
      color: #1a2980;
      text-decoration: none;
      font-size: 14px;
      font-weight: 700;
      padding: 8px 20px;
      border-radius: 8px;
      background: white;
      transition: all 0.2s ease;

      &:hover {
        background: #f0f4ff;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      }
    }

    /* Mobile toggle */
    .mobile-toggle {
      display: none;
      background: none;
      border: none;
      color: white;
      cursor: pointer;
      padding: 6px;
      border-radius: 8px;
      transition: background 0.2s;

      &:hover {
        background: rgba(255,255,255,0.1);
      }

      mat-icon {
        font-size: 24px;
      }
    }

    /* Mobile nav */
    .mobile-nav {
      display: none;
      flex-direction: column;
      background: rgba(15, 26, 92, 0.98);
      border-top: 1px solid rgba(255,255,255,0.1);
      padding: 12px;
      gap: 4px;
    }

    .mobile-nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      color: rgba(255,255,255,0.85);
      text-decoration: none;
      font-size: 15px;
      font-weight: 500;
      padding: 12px 16px;
      border-radius: 10px;
      transition: all 0.2s ease;

      mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }

      &:hover {
        background: rgba(255,255,255,0.1);
        color: white;
      }
    }

    .mobile-logout {
      display: flex;
      align-items: center;
      gap: 12px;
      color: #e74c3c;
      background: none;
      border: none;
      font-size: 15px;
      font-weight: 500;
      padding: 12px 16px;
      border-radius: 10px;
      cursor: pointer;
      width: 100%;
      transition: background 0.2s;

      mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }

      &:hover {
        background: rgba(231,76,60,0.1);
      }
    }

    /* Responsive */
    @media (max-width: 900px) {
      .nav-app {
        display: none;
      }

      .mobile-toggle {
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .mobile-nav {
        display: flex;
      }

      .xp-badge, .level-badge {
        display: none;
      }
    }

    @media (max-width: 640px) {
      .nav-public {
        display: none;
      }

      .header-inner {
        padding: 0 16px;
      }
    }
  `]
})
export class HeaderComponent implements OnInit, OnDestroy {
  isDarkMode = false;
  isAuthPage = false;
  isScrolled = false;
  mobileMenuOpen = false;
  userXp = 0;
  userLevel = 1;
  userAvatar = '🎓';

  private routerSub = Subscription.EMPTY;
  private userSub = Subscription.EMPTY;

  constructor(private router: Router, private authService: AuthService) {
    this.routerSub = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      this.isAuthPage = event.url.includes('/auth') || event.url.includes('/login') || event.url.includes('/register');
      this.mobileMenuOpen = false;
    });
  }

  ngOnInit(): void {
    this.isAuthPage = window.location.href.includes('/auth') || window.location.href.includes('/login');
    this.loadUserStats();

    // Mantém XP/nível sincronizados com o estado de autenticação
    this.userSub = this.authService.currentUser$.subscribe(user => {
      this.userXp = user?.xp ?? 0;
      this.userLevel = user?.level ?? 1;
      this.userAvatar = user?.avatar || '🎓';
    });
  }

  ngOnDestroy(): void {
    this.routerSub.unsubscribe();
    this.userSub.unsubscribe();
  }

  private loadUserStats(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.userXp = user.xp ?? 0;
      this.userLevel = user.level ?? 1;
      this.userAvatar = user.avatar || this.authService.getAvatar() || '🎓';
    }
  }

  @HostListener('window:scroll')
  onScroll(): void {
    this.isScrolled = window.scrollY > 20;
  }

  isAuthenticated(): boolean {
    return this.authService.isAuthenticated();
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen = false;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}