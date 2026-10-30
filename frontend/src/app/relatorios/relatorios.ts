import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatGridListModule } from '@angular/material/grid-list';
import { firstValueFrom } from 'rxjs';
import { ExerciseService, ExerciseStats, ExerciseHistory } from '../services/exercise.service';
import { AuthService } from '../services/auth.service';

interface ChartData {
  labels: string[];
  data: number[];
  colors: string[];
}

interface StatCard {
  title: string;
  value: string | number;
  icon: string;
  color: string;
  trend?: number;
}

const PALETTE = [
  '#2196f3', '#4caf50', '#ff9800', '#9c27b0',
  '#f44336', '#00bcd4', '#795548', '#607d8b'
];

@Component({
  selector: 'app-relatorios',
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatGridListModule
  ],
  templateUrl: './relatorios.html',
  styleUrl: './relatorios.scss',
})
export class Relatorios implements OnInit, OnDestroy {
  Math = Math;

  private animationTimers: ReturnType<typeof setInterval>[] = [];

  constructor(
    private exerciseService: ExerciseService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  statCards: StatCard[] = [
    {
      title: 'Questões Respondidas',
      value: 892,
      icon: 'quiz',
      color: '#2196f3',
      trend: 15.2
    },
    {
      title: 'Lições Completadas',
      value: 32,
      icon: 'school',
      color: '#4caf50',
      trend: 12.5
    },
    {
      title: 'Taxa de Acerto',
      value: '87%',
      icon: 'check_circle',
      color: '#ff9800',
      trend: 3.5
    },
    {
      title: 'Dias de Streak',
      value: 12,
      icon: 'local_fire_department',
      color: '#9c27b0',
      trend: 20.0
    }
  ];

  userProgressData = {
    labels: ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun'],
    data: [120, 190, 300, 500, 420, 670],
    colors: ['#2196f3', '#4caf50', '#ff9800', '#9c27b0', '#f44336', '#00bcd4']
  };

  contentByCategory = {
    labels: ['Matemática', 'Português', 'Física', 'Química', 'Biologia'],
    data: [87, 92, 78, 85, 90],
    colors: ['#2196f3', '#4caf50', '#ff9800', '#9c27b0', '#f44336']
  };

  quizPerformance = {
    labels: ['Álgebra', 'Geometria', 'Química Orgânica', 'Gramática'],
    data: [65, 58, 72, 81],
    colors: ['#f44336', '#ff9800', '#2196f3', '#4caf50']
  };

  recentActivities: { user: string; action: string; time: string; icon: string; color: string }[] = [];

  ngOnInit(): void {
    this.loadData();
  }

  ngOnDestroy(): void {
    // Cancela animações pendentes para não disparar timers contra um componente
    // já destruído (e não vazar intervalos).
    this.animationTimers.forEach(timer => clearInterval(timer));
    this.animationTimers = [];
  }

  private async loadData(): Promise<void> {
    try {
      const stats = await firstValueFrom(this.exerciseService.getExerciseStats());
      this.applyStats(stats);
    } catch {
      // mantém fallback mocado e anima os números
      this.animateNumbers();
    }

    try {
      const history = await firstValueFrom(this.exerciseService.getExerciseHistory());
      this.applyRecentActivities(history);
      this.applyProgress(history);
    } catch {
      // mantém as atividades mockadas
    }
  }

  private applyRecentActivities(history: ExerciseHistory[]): void {
    // Sem histórico (ou lista vazia), deixa a lista vazia — nunca exibe dados fictícios.
    const myName = this.authService.getCurrentUser()?.name ?? 'Você';
    this.recentActivities = (history || []).slice(0, 6).map(h => {
      if (h.isSession) {
        const acertos = h.correctAnswers ?? 0;
        const total = h.totalQuestions ?? 0;
        return {
          user: myName,
          action: `Completou uma sessão de ${h.category} (${acertos}/${total} acertos)`,
          time: this.relativeTime(h.timestamp),
          icon: total > 0 && acertos >= total ? 'check_circle' : (acertos > 0 ? 'trending_up' : 'cancel'),
          color: acertos > 0 ? '#4caf50' : '#f44336'
        };
      }
      const correct = !!h.correct;
      return {
        user: myName,
        action: correct
          ? `Acertou a questão de ${h.category}`
          : `Errou a questão de ${h.category}`,
        time: this.relativeTime(h.timestamp),
        icon: correct ? 'check_circle' : 'cancel',
        color: correct ? '#4caf50' : '#f44336'
      };
    });
    this.cdr.detectChanges();
  }

  private relativeTime(timestamp: string): string {
    const then = new Date(timestamp).getTime();
    if (isNaN(then)) return 'agora';

    const diffMs = Date.now() - then;
    const minutes = Math.floor(diffMs / 60000);
    if (minutes < 1) return 'agora mesmo';
    if (minutes < 60) return `${minutes} minuto${minutes === 1 ? '' : 's'} atrás`;

    const hours = Math.floor(minutes / 60);
    if (hours < 24) return `${hours} hora${hours === 1 ? '' : 's'} atrás`;

    const days = Math.floor(hours / 24);
    return `${days} dia${days === 1 ? '' : 's'} atrás`;
  }

  /**
   * Deriva do histórico real: o total de "Lições Completadas" (questões distintas
   * respondidas corretamente) e a série mensal de questões respondidas.
   */
  private applyProgress(history: ExerciseHistory[]): void {
    const items = history || [];
    // Conta questões DISTINTAS acertadas (o enunciado identifica a questão; o id
    // do histórico é o id da resposta, único por submissão, então não serve).
    const distinctQuestions = new Set(
      items.filter(h => !h.isSession && h.correct).map(h => h.question)
    ).size;
    const distinctSessions = items.filter(h => h.isSession && (h.correctAnswers ?? 0) > 0).length;
    const distinctCorrect = distinctQuestions + distinctSessions;

    this.statCards = this.statCards.map(card =>
      card.title === 'Lições Completadas' ? { ...card, value: distinctCorrect } : card
    );

    const monthKeys = this.lastSixMonths();
    const counts = new Array<number>(monthKeys.length).fill(0);
    for (const h of (history || [])) {
      const idx = monthKeys.indexOf(this.monthKey(h.timestamp));
      if (idx >= 0) counts[idx]++;
    }

    this.userProgressData = {
      labels: monthKeys.map(k => this.monthLabel(k)),
      data: counts,
      colors: monthKeys.map((_, i) => PALETTE[i % PALETTE.length])
    };

    this.cdr.detectChanges();
  }

  private lastSixMonths(): string[] {
    const keys: string[] = [];
    const now = new Date();
    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      keys.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
    }
    return keys;
  }

  private monthKey(timestamp: string): string {
    const d = new Date(timestamp);
    if (isNaN(d.getTime())) return '';
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  }

  private monthLabel(key: string): string {
    const names = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
    const month = Number(key.split('-')[1]) || 1;
    return names[month - 1];
  }

  private applyStats(stats: ExerciseStats): void {
    // Atualiza apenas os valores, preservando título/ícone/cor/trend originais.
    this.statCards = this.statCards.map(card => {
      switch (card.title) {
        case 'Questões Respondidas':
          return { ...card, value: stats.totalExercises };
        case 'Taxa de Acerto':
          return { ...card, value: `${Math.round(stats.accuracyRate)}%` };
        case 'Dias de Streak':
          return { ...card, value: this.authService.getUserStats().streak };
        default:
          // "Lições Completadas" permanece mocado (sem equivalente no backend).
          return card;
      }
    });

    const categories = stats.categoryStats
      ? Object.entries(stats.categoryStats)
          .filter(([, s]) => (s.total ?? 0) > 0)
          .sort((a, b) => b[1].accuracy - a[1].accuracy)
      : [];

    if (categories.length > 0) {
      this.contentByCategory = {
        labels: categories.map(([name]) => name),
        data: categories.map(([, s]) => Math.round(s.accuracy)),
        colors: categories.map((_, i) => PALETTE[i % PALETTE.length])
      };

      const weakest = [...categories].sort((a, b) => a[1].accuracy - b[1].accuracy).slice(0, 4);
      this.quizPerformance = {
        labels: weakest.map(([name]) => name),
        data: weakest.map(([, s]) => Math.round(s.accuracy)),
        colors: ['#f44336', '#ff9800', '#2196f3', '#4caf50']
      };
    }

    this.cdr.detectChanges();
  }

  animateNumbers(): void {
    this.statCards.forEach((card) => {
      if (typeof card.value === 'number') {
        const targetValue = card.value;
        let currentValue = 0;
        const increment = targetValue / 50;

        const timer = setInterval(() => {
          currentValue += increment;
          if (currentValue >= targetValue) {
            currentValue = targetValue;
            clearInterval(timer);
          }
          card.value = Math.floor(currentValue);
        }, 30);
        this.animationTimers.push(timer);
      }
    });
  }

  getTrendIcon(trend: number): string {
    return trend > 0 ? 'trending_up' : 'trending_down';
  }

  getTrendColor(trend: number): string {
    return trend > 0 ? '#4caf50' : '#f44336';
  }

  getChartStyle(data: number[], index: number): any {
    const maxValue = data.length > 0 ? Math.max(...data) : 0;
    const height = maxValue > 0 ? (data[index] / maxValue) * 100 : 0;
    return {
      height: `${height}%`,
      backgroundColor: this.userProgressData.colors[index]
    };
  }
}
