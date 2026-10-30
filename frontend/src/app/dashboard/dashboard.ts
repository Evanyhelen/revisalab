import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { firstValueFrom } from 'rxjs';
import { DashboardService } from '../services/dashboard.service';
import { TrailService } from '../services/trail.service';
import { GamificationService } from '../services/gamification.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: false,
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class Dashboard implements OnInit {
  sidebarOpen = true;

  // Cabeçalho / cartão de XP (preenchidos pelo backend, com fallback mocado)
  userName = 'Estudante';
  streak = 12;
  level = 5;
  xp = 1240;
  nextLevelXp = 1500;
  xpPercent = 82;
  levelTitle = 'Estudante Avançado';
  questionsCount = 247;
  accuracy = '87%';
  studyHours = '14h';

  constructor(
    private dashboardService: DashboardService,
    private trailService: TrailService,
    private gamificationService: GamificationService,
    private authService: AuthService
  ) {
    const savedState = localStorage.getItem('sidebarOpen');
    if (savedState !== null) {
      this.sidebarOpen = savedState === 'true';
    }
  }

  // Valores mocados servem apenas de fallback (backend indisponível).
  trails = [
    { icon: 'calculate', name: 'Matemática Básica', progress: 62, nextLesson: 'Equações do 2º grau', color: '#1e3a8a' },
    { icon: 'menu_book', name: 'Interpretação de Texto', progress: 35, nextLesson: 'Gêneros textuais', color: '#0ea5e9' },
    { icon: 'psychology', name: 'Raciocínio Lógico', progress: 18, nextLesson: 'Proposições condicionais', color: '#8b5cf6' }
  ];

  missions = [
    { title: 'Completar 5 questões hoje', xp: 25, done: true, icon: '🎯' },
    { title: 'Estudar por 30 minutos', xp: 20, done: true, icon: '⏱️' },
    { title: 'Completar 1 lição de Matemática', xp: 40, done: false, icon: '📐' },
    { title: 'Manter o streak diário', xp: 10, done: false, icon: '🔥' }
  ];

  // Prévia do ranking na home: apenas os 4 primeiros. A lista completa fica em /ranking.
  ranking = [
    { emoji: '👩', name: 'Maria Eduarda', xp: 2450, isMe: false },
    { emoji: '👨', name: 'João Pedro', xp: 2210, isMe: false },
    { emoji: '🎓', name: 'Você', xp: 1240, isMe: true },
    { emoji: '👩', name: 'Ana Carolina', xp: 1100, isMe: false }
  ];

  badgesRecent = [
    { icon: '🥇', name: 'Primeiro Passo', locked: false },
    { icon: '🔥', name: 'Constância', locked: false },
    { icon: '🗺️', name: 'Explorador', locked: false },
    { icon: '🎯', name: 'Precisão', locked: true },
    { icon: '🏆', name: 'Foco Total', locked: true },
    { icon: '⚡', name: 'Velocista', locked: true }
  ];

  ngOnInit(): void {
    this.loadData();
  }

  /** Busca os dados reais do backend; em caso de falha mantém os valores mocados. */
  private async loadData(): Promise<void> {
    const currentUser = this.authService.getCurrentUser();

    try {
      const stats = await firstValueFrom(this.dashboardService.getDashboardStats());
      this.userName = stats.name ?? 'Estudante';
      this.streak = stats.streak ?? 0;
      this.level = stats.level ?? 1;
      this.xp = stats.xp ?? 0;
      this.questionsCount = stats.totalExercises ?? 0;
      this.accuracy = `${Math.round(stats.accuracyRate ?? 0)}%`;
      this.studyHours = `${stats.totalStudyTime ?? 0}h`;

      // Progresso de XP (próximo nível a cada 1000 XP).
      const threshold = (this.level) * 1000;
      this.nextLevelXp = threshold;
      this.xpPercent = Math.min(100, Math.round(((this.xp % 1000) / 1000) * 100));
      this.levelTitle = this.titleForLevel(this.level);
    } catch {
      // mantém fallback mocado
    }

    try {
      const trails = await firstValueFrom(this.trailService.getAllTrails());
      const mapped = trails
        .filter(t => (t.progress ?? 0) > 0)
        .sort((a, b) => (b.progress ?? 0) - (a.progress ?? 0))
        .map(t => ({
          icon: t.icon || 'route',
          name: t.title,
          progress: Math.round((t.progress ?? 0) * 100),
          nextLesson: t.category,
          color: t.color || '#1e3a8a'
        }));
      if (mapped.length > 0) {
        this.trails = mapped.slice(0, 3);
      }
    } catch {
      // mantém fallback mocado
    }

    try {
      const missions = await firstValueFrom(this.gamificationService.getUserMissions());
      this.missions = missions.map(m => ({
        title: m.title,
        xp: m.xpReward ?? 0,
        done: m.completed ?? false,
        icon: this.missionEmoji(m.actionType)
      }));
    } catch {
      // mantém fallback mocado
    }

    try {
      const leaderboard = await firstValueFrom(this.dashboardService.getLeaderboard());
      const myId = currentUser?.id;
      this.ranking = leaderboard.map((u, i) => ({
        emoji: this.rankAvatar(i),
        name: u.name,
        xp: u.xp,
        isMe: u.id === myId
      })).slice(0, 4);
    } catch {
      // mantém fallback mocado
    }

    try {
      const achievements = await firstValueFrom(this.gamificationService.getUserAchievements());
      const unlocked = achievements.filter(a => a.unlocked);
      const locked = achievements.filter(a => !a.unlocked);
      this.badgesRecent = [...unlocked, ...locked].slice(0, 6).map(a => ({
        icon: this.achievementEmoji(a.type),
        name: a.name,
        locked: !a.unlocked
      }));
    } catch {
      // mantém fallback mocado
    }
  }

  private titleForLevel(level: number): string {
    if (level >= 15) return 'Lenda do Estudo';
    if (level >= 10) return 'Mestre';
    if (level >= 5) return 'Estudante Avançado';
    if (level >= 3) return 'Estudante Dedicado';
    return 'Estudante Iniciante';
  }

  private rankAvatar(index: number): string {
    return index === 0 ? '🥇' : index === 1 ? '🥈' : index === 2 ? '🥉' : '🎓';
  }

  private missionEmoji(actionType: string): string {
    const map: Record<string, string> = {
      complete_exercises: '🎯',
      study_minutes: '⏱️',
      complete_trail: '📐',
      streak: '🔥',
      correct_streak: '⚡',
      review_errors: '🔁'
    };
    return map[actionType] ?? '⭐';
  }

  private achievementEmoji(type: string): string {
    const map: Record<string, string> = {
      streak: '🔥',
      level: '🏆',
      exercise: '🎯',
      trail: '🗺️',
      custom: '⭐'
    };
    return map[type] ?? '⭐';
  }

  // Dados sincronizados com as seções
  get totalBadges(): number {
    return this.badgesRecent.filter(b => !b.locked).length;
  }

  get completedMissions(): number {
    return this.missions.filter(m => m.done).length;
  }

  /** Percentual de missões concluídas; 0 quando não há missões (evita NaN/divisão por zero). */
  get missionsPercentage(): number {
    if (!this.missions.length) return 0;
    return (this.completedMissions / this.missions.length) * 100;
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
    localStorage.setItem('sidebarOpen', this.sidebarOpen.toString());
  }
}
