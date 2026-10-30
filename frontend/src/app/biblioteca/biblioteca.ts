import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterModule } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { GamificationService, Achievement as AchievementDTO } from '../services/gamification.service';

interface Badge {
  id: string;
  name: string;
  description: string;
  icon: string;
  rarity: 'comum' | 'raro' | 'epico' | 'lendario';
  unlocked: boolean;
  unlockedAt?: Date;
  progress?: number;
  maxProgress?: number;
  category: string;
}

@Component({
  selector: 'app-biblioteca',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTabsModule,
    MatProgressBarModule,
    RouterModule
  ],
  templateUrl: './biblioteca.html',
  styleUrls: ['./biblioteca.scss'],
})
export class Biblioteca implements OnInit {
  sidebarOpen = true;
  selectedCategory: 'todas' | 'aprendizado' | 'desempenho' | 'social' | 'especial' = 'todas';

  constructor(private gamificationService: GamificationService) {}

  // Fallback mocado (backend indisponível).
  badges: Badge[] = [
    { id: '1', name: 'Primeiro Passo', description: 'Complete sua primeira lição', icon: '🎯', rarity: 'comum', unlocked: true, unlockedAt: new Date('2025-01-15'), category: 'aprendizado' },
    { id: '2', name: 'Estudante Dedicado', description: 'Complete 10 lições', icon: '📚', rarity: 'comum', unlocked: true, unlockedAt: new Date('2025-01-20'), category: 'aprendizado' },
    { id: '3', name: 'Mestre do Conhecimento', description: 'Complete 50 lições', icon: '🎓', rarity: 'raro', unlocked: false, progress: 32, maxProgress: 50, category: 'aprendizado' },
    { id: '4', name: 'Sábio', description: 'Complete 100 lições', icon: '🧙', rarity: 'epico', unlocked: false, progress: 32, maxProgress: 100, category: 'aprendizado' },
    { id: '5', name: 'Polímata', description: 'Complete pelo menos 1 trilha de cada área', icon: '🌟', rarity: 'lendario', unlocked: false, progress: 3, maxProgress: 11, category: 'aprendizado' },
    { id: '6', name: 'Acertou!', description: 'Acerte 10 questões seguidas', icon: '✅', rarity: 'comum', unlocked: true, unlockedAt: new Date('2025-01-18'), category: 'desempenho' },
    { id: '7', name: 'Precisão Cirúrgica', description: 'Mantenha 90% de acerto em 20 questões', icon: '🎯', rarity: 'raro', unlocked: false, progress: 14, maxProgress: 20, category: 'desempenho' },
    { id: '8', name: 'Perfeição', description: 'Acerte 50 questões seguidas', icon: '💯', rarity: 'epico', unlocked: false, progress: 8, maxProgress: 50, category: 'desempenho' },
    { id: '9', name: 'Gênio', description: 'Alcance 95% de taxa de acerto geral', icon: '🧠', rarity: 'lendario', unlocked: false, progress: 87, maxProgress: 95, category: 'desempenho' },
    { id: '10', name: 'Velocista', description: 'Complete 20 questões em menos de 10 minutos', icon: '⚡', rarity: 'raro', unlocked: true, unlockedAt: new Date('2025-02-01'), category: 'desempenho' },
    { id: '11', name: 'Competidor', description: 'Entre no ranking semanal', icon: '🏅', rarity: 'comum', unlocked: true, unlockedAt: new Date('2025-01-16'), category: 'social' },
    { id: '12', name: 'Top 10', description: 'Fique entre os 10 primeiros do ranking semanal', icon: '🏆', rarity: 'raro', unlocked: false, progress: 15, maxProgress: 10, category: 'social' },
    { id: '13', name: 'Campeão', description: 'Fique em 1º lugar no ranking semanal', icon: '👑', rarity: 'epico', unlocked: false, category: 'social' },
    { id: '14', name: 'Lenda', description: 'Fique em 1º lugar no ranking geral', icon: '🌟', rarity: 'lendario', unlocked: false, category: 'social' },
    { id: '15', name: 'Madrugador', description: 'Estude antes das 6h da manhã', icon: '🌅', rarity: 'comum', unlocked: true, unlockedAt: new Date('2025-01-22'), category: 'especial' },
    { id: '16', name: 'Coruja', description: 'Estude depois das 23h', icon: '🦉', rarity: 'comum', unlocked: false, category: 'especial' },
    { id: '17', name: 'Maratonista', description: 'Estude por 3 horas seguidas', icon: '🏃', rarity: 'raro', unlocked: false, category: 'especial' },
    { id: '18', name: 'Incansável', description: 'Mantenha 30 dias de streak', icon: '🔥', rarity: 'epico', unlocked: false, progress: 12, maxProgress: 30, category: 'especial' },
    { id: '19', name: 'Imortal', description: 'Mantenha 100 dias de streak', icon: '💎', rarity: 'lendario', unlocked: false, progress: 12, maxProgress: 100, category: 'especial' },
    { id: '20', name: 'Explorador', description: 'Visite todas as seções da plataforma', icon: '🗺️', rarity: 'comum', unlocked: true, unlockedAt: new Date('2025-01-17'), category: 'especial' }
  ];

  ngOnInit(): void {
    const savedState = localStorage.getItem('sidebarOpen');
    if (savedState !== null) {
      this.sidebarOpen = savedState === 'true';
    }
    this.loadAchievements();
  }

  private async loadAchievements(): Promise<void> {
    try {
      const achievements = await firstValueFrom(this.gamificationService.getUserAchievements());
      this.badges = achievements.map(a => this.toBadge(a));
    } catch {
      // mantém fallback mocado
    }
  }

  private toBadge(a: AchievementDTO): Badge {
    return {
      id: String(a.id),
      name: a.name,
      description: a.description,
      icon: this.emojiFor(a.type),
      rarity: this.rarityFor(a.requiredValue ?? 0),
      unlocked: a.unlocked,
      unlockedAt: a.unlockedAt ? new Date(a.unlockedAt) : undefined,
      progress: a.progress,
      maxProgress: a.requiredValue,
      category: this.categoryFor(a.type)
    };
  }

  private emojiFor(type: string): string {
    const map: Record<string, string> = {
      streak: '🔥',
      level: '🏆',
      exercise: '🎯',
      trail: '🗺️',
      custom: '⭐'
    };
    return map[type] ?? '⭐';
  }

  private categoryFor(type: string): string {
    const map: Record<string, string> = {
      streak: 'desempenho',
      level: 'aprendizado',
      exercise: 'aprendizado',
      trail: 'aprendizado',
      custom: 'especial'
    };
    return map[type] ?? 'especial';
  }

  private rarityFor(requiredValue: number): 'comum' | 'raro' | 'epico' | 'lendario' {
    if (requiredValue <= 3) return 'comum';
    if (requiredValue <= 10) return 'raro';
    if (requiredValue <= 50) return 'epico';
    return 'lendario';
  }

  get filteredBadges(): Badge[] {
    if (this.selectedCategory === 'todas') {
      return this.badges;
    }
    return this.badges.filter(b => b.category === this.selectedCategory);
  }

  get unlockedCount(): number {
    return this.badges.filter(b => b.unlocked).length;
  }

  get totalBadges(): number {
    return this.badges.length;
  }

  get completionPercentage(): number {
    return this.totalBadges === 0 ? 0 : Math.round((this.unlockedCount / this.totalBadges) * 100);
  }

  selectCategory(category: 'todas' | 'aprendizado' | 'desempenho' | 'social' | 'especial'): void {
    this.selectedCategory = category;
  }

  getRarityClass(rarity: string): string {
    return `rarity-${rarity}`;
  }

  getRarityColor(rarity: string): string {
    switch(rarity) {
      case 'comum': return '#95a5a6';
      case 'raro': return '#3498db';
      case 'epico': return '#9b59b6';
      case 'lendario': return '#f39c12';
      default: return '#95a5a6';
    }
  }

  getProgressPercentage(badge: Badge): number {
    if (!badge.progress || !badge.maxProgress) return 0;
    return Math.round((badge.progress / badge.maxProgress) * 100);
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
    localStorage.setItem('sidebarOpen', this.sidebarOpen.toString());
  }
}
