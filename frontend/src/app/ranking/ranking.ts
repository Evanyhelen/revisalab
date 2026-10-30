import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { firstValueFrom } from 'rxjs';
import { GamificationService, LeaderboardEntry } from '../services/gamification.service';
import { AuthService } from '../services/auth.service';

interface RankingUser {
  position: number;
  name: string;
  level: number;
  xp: number;
  streak: number;
  badges: number;
  avatar: string;
}

@Component({
  selector: 'app-ranking',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTableModule,
    MatTabsModule,
    RouterModule
  ],
  templateUrl: './ranking.html',
  styleUrl: './ranking.scss',
})
export class RankingComponent implements OnInit {
  sidebarOpen = true;
  selectedPeriod: 'semanal' | 'mensal' | 'geral' = 'semanal';

  constructor(
    private gamificationService: GamificationService,
    private authService: AuthService
  ) {}

  // Fallback mocado (backend indisponível).
  rankingSemanal: RankingUser[] = [
    { position: 1, name: 'Ana Carolina Silva', level: 8, xp: 2450, streak: 15, badges: 12, avatar: '👩' },
    { position: 2, name: 'João Pedro Santos', level: 7, xp: 2180, streak: 12, badges: 10, avatar: '👨' },
    { position: 3, name: 'Maria Eduarda Lima', level: 7, xp: 2050, streak: 10, badges: 9, avatar: '👩' },
    { position: 4, name: 'Lucas Oliveira', level: 6, xp: 1890, streak: 8, badges: 8, avatar: '👨' },
    { position: 5, name: 'Beatriz Ferreira', level: 6, xp: 1750, streak: 14, badges: 11, avatar: '👩' },
    { position: 6, name: 'Rafael Costa', level: 5, xp: 1620, streak: 7, badges: 7, avatar: '👨' },
    { position: 7, name: 'Juliana Alves', level: 5, xp: 1480, streak: 9, badges: 6, avatar: '👩' },
    { position: 8, name: 'Pedro Henrique', level: 5, xp: 1350, streak: 6, badges: 6, avatar: '👨' },
    { position: 9, name: 'Camila Rodrigues', level: 4, xp: 1220, streak: 11, badges: 5, avatar: '👩' },
    { position: 10, name: 'Gabriel Souza', level: 4, xp: 1100, streak: 5, badges: 5, avatar: '👨' }
  ];

  rankingMensal: RankingUser[] = [
    { position: 1, name: 'Maria Eduarda Lima', level: 8, xp: 8950, streak: 28, badges: 15, avatar: '👩' },
    { position: 2, name: 'João Pedro Santos', level: 7, xp: 7680, streak: 25, badges: 13, avatar: '👨' },
    { position: 3, name: 'Ana Carolina Silva', level: 7, xp: 7250, streak: 22, badges: 12, avatar: '👩' },
    { position: 4, name: 'Beatriz Ferreira', level: 6, xp: 6890, streak: 30, badges: 14, avatar: '👩' },
    { position: 5, name: 'Lucas Oliveira', level: 6, xp: 6420, streak: 20, badges: 11, avatar: '👨' },
    { position: 6, name: 'Rafael Costa', level: 5, xp: 5780, streak: 18, badges: 10, avatar: '👨' },
    { position: 7, name: 'Camila Rodrigues', level: 5, xp: 5340, streak: 26, badges: 9, avatar: '👩' },
    { position: 8, name: 'Gabriel Souza', level: 5, xp: 4920, streak: 15, badges: 8, avatar: '👨' },
    { position: 9, name: 'Juliana Alves', level: 4, xp: 4580, streak: 19, badges: 8, avatar: '👩' },
    { position: 10, name: 'Pedro Henrique', level: 4, xp: 4150, streak: 12, badges: 7, avatar: '👨' }
  ];

  rankingGeral: RankingUser[] = [
    { position: 1, name: 'João Pedro Santos', level: 12, xp: 15680, streak: 45, badges: 22, avatar: '👨' },
    { position: 2, name: 'Maria Eduarda Lima', level: 11, xp: 14250, streak: 42, badges: 20, avatar: '👩' },
    { position: 3, name: 'Beatriz Ferreira', level: 10, xp: 12890, streak: 50, badges: 21, avatar: '👩' },
    { position: 4, name: 'Ana Carolina Silva', level: 10, xp: 12450, streak: 38, badges: 19, avatar: '👩' },
    { position: 5, name: 'Lucas Oliveira', level: 9, xp: 11220, streak: 35, badges: 17, avatar: '👨' },
    { position: 6, name: 'Rafael Costa', level: 8, xp: 9780, streak: 32, badges: 15, avatar: '👨' },
    { position: 7, name: 'Camila Rodrigues', level: 8, xp: 9340, streak: 40, badges: 16, avatar: '👩' },
    { position: 8, name: 'Gabriel Souza', level: 7, xp: 8520, streak: 28, badges: 14, avatar: '👨' },
    { position: 9, name: 'Juliana Alves', level: 7, xp: 7980, streak: 30, badges: 13, avatar: '👩' },
    { position: 10, name: 'Pedro Henrique', level: 6, xp: 7150, streak: 25, badges: 12, avatar: '👨' }
  ];

  currentRanking: RankingUser[] = [];
  userPosition: RankingUser = {
    position: 15,
    name: 'Você',
    level: 5,
    xp: 1240,
    streak: 12,
    badges: 7,
    avatar: '🎓'
  };

  // Invalida respostas de ranking fora de ordem (trocas rápidas de aba).
  private rankSeq = 0;

  ngOnInit(): void {
    this.updateRanking();

    const savedState = localStorage.getItem('sidebarOpen');
    if (savedState !== null) {
      this.sidebarOpen = savedState === 'true';
    }
  }

  updateRanking(): void {
    // Fallback mocado imediato.
    switch (this.selectedPeriod) {
      case 'semanal': this.currentRanking = this.rankingSemanal; break;
      case 'mensal': this.currentRanking = this.rankingMensal; break;
      case 'geral': this.currentRanking = this.rankingGeral; break;
    }
    this.loadRealRanking(this.sortByForPeriod(this.selectedPeriod));
  }

  private sortByForPeriod(period: 'semanal' | 'mensal' | 'geral'): string {
    switch (period) {
      case 'semanal': return 'xp';
      case 'mensal': return 'streak';
      case 'geral': return 'level';
      default: return 'xp';
    }
  }

  private async loadRealRanking(sortBy: string): Promise<void> {
    const seq = ++this.rankSeq;
    try {
      const res = await firstValueFrom(this.gamificationService.getRanking(sortBy, 20));
      if (seq !== this.rankSeq) return; // uma troca de aba mais recente já assumiu
      const me = this.authService.getCurrentUser();
      const mapped = res.ranking.map((u: LeaderboardEntry, i: number) => this.toRankingUser(u, i));
      if (mapped.length > 0) {
        this.currentRanking = mapped;
        const found = mapped.find(u => u.name === me?.name);
        if (found) {
          this.userPosition = found;
        } else if (me) {
          // Usuário fora do top-20: exibe seus próprios dados reais, não os do último colocado.
          this.userPosition = {
            position: mapped.length + 1,
            name: me.name,
            level: me.level,
            xp: me.xp,
            streak: me.streak,
            badges: Math.round((me.level ?? 1) * 1.5),
            avatar: '🎓'
          };
        }
      }
    } catch {
      // mantém fallback mocado
    }
  }

  private toRankingUser(u: LeaderboardEntry, index: number): RankingUser {
    const position = u.position ?? index + 1;
    return {
      position,
      name: u.name,
      level: u.level ?? 1,
      xp: u.xp ?? 0,
      streak: u.streak ?? 0,
      // O backend ainda não expõe contagem de badges por usuário; valor estimado.
      badges: Math.round((u.level ?? 1) * 1.5),
      avatar: this.avatarFor(position)
    };
  }

  private avatarFor(position: number): string {
    if (position === 1) return '🥇';
    if (position === 2) return '🥈';
    if (position === 3) return '🥉';
    return '🎓';
  }

  selectPeriod(period: 'semanal' | 'mensal' | 'geral'): void {
    this.selectedPeriod = period;
    this.updateRanking();
  }

  getMedalIcon(position: number): string {
    switch(position) {
      case 1: return '🥇';
      case 2: return '🥈';
      case 3: return '🥉';
      default: return '';
    }
  }

  getMedalClass(position: number): string {
    switch(position) {
      case 1: return 'gold';
      case 2: return 'silver';
      case 3: return 'bronze';
      default: return '';
    }
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
    localStorage.setItem('sidebarOpen', this.sidebarOpen.toString());
  }
}
