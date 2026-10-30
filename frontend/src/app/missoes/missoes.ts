import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { GamificationService, Mission as MissionDTO, PersonalMissionRequest } from '../services/gamification.service';

interface Mission {
  id: number;
  title: string;
  description: string;
  xp: number;
  done: boolean;
  claimable: boolean;
  progress: number;
  requiredValue: number;
  icon: string;
  category: string;
  personal: boolean;
}

@Component({
  selector: 'app-missoes',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatIconModule,
    MatCheckboxModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    FormsModule
  ],
  templateUrl: './missoes.html',
  styleUrl: './missoes.scss',
})
export class MissoesComponent implements OnInit {
  sidebarOpen = true;

  constructor(
    private gamificationService: GamificationService,
    private toastr: ToastrService
  ) {
    const savedState = localStorage.getItem('sidebarOpen');
    if (savedState !== null) {
      this.sidebarOpen = savedState === 'true';
    }
  }

  // Fallback mocado (backend indisponível).
  missions: Mission[] = [
    { id: 1, title: 'Completar 5 questões hoje', description: 'Responda corretamente 5 questões de qualquer trilha', xp: 25, done: true, claimable: false, progress: 5, requiredValue: 5, icon: 'quiz', category: 'Prática', personal: false },
    { id: 2, title: 'Estudar por 30 minutos', description: 'Dedique pelo menos 30 minutos aos estudos hoje', xp: 20, done: true, claimable: false, progress: 30, requiredValue: 30, icon: 'schedule', category: 'Tempo', personal: false },
    { id: 3, title: 'Completar 1 lição de Matemática', description: 'Finalize uma lição completa da trilha de Matemática', xp: 40, done: false, claimable: false, progress: 0, requiredValue: 1, icon: 'calculate', category: 'Trilha', personal: false },
    { id: 4, title: 'Manter o streak diário', description: 'Acesse a plataforma e estude por pelo menos 10 minutos', xp: 10, done: false, claimable: false, progress: 0, requiredValue: 1, icon: 'local_fire_department', category: 'Streak', personal: false },
    { id: 5, title: 'Acertar 10 questões seguidas', description: 'Demonstre domínio acertando 10 questões consecutivas', xp: 50, done: false, claimable: false, progress: 0, requiredValue: 10, icon: 'emoji_events', category: 'Desafio', personal: false },
    { id: 6, title: 'Revisar erros anteriores', description: 'Revise e refaça 3 questões que você errou anteriormente', xp: 30, done: false, claimable: false, progress: 0, requiredValue: 3, icon: 'refresh', category: 'Revisão', personal: false }
  ];

  // Formulário de missão pessoal.
  showPersonalForm = false;
  creatingMission = false;
  personalForm = {
    title: '',
    description: '',
    type: 'daily' as 'daily' | 'weekly',
    actionType: 'complete_exercises' as 'complete_exercises' | 'study_minutes' | 'streak',
    requiredValue: 5,
    xpReward: 25
  };

  ngOnInit(): void {
    this.loadMissions();
  }

  private async loadMissions(): Promise<void> {
    try {
      const missions = await firstValueFrom(this.gamificationService.getUserMissions());
      this.missions = missions.map(m => this.toMission(m));
    } catch {
      // mantém fallback mocado
    }
  }

  private toMission(m: MissionDTO): Mission {
    return {
      id: m.id,
      title: m.title,
      description: m.description,
      xp: m.xpReward ?? 0,
      done: m.completed ?? false,
      claimable: m.claimable ?? false,
      progress: m.progress ?? 0,
      requiredValue: m.requiredValue ?? 0,
      icon: this.iconFor(m.actionType),
      category: this.categoryFor(m.actionType),
      personal: m.personal ?? false
    };
  }

  async createPersonalMission(): Promise<void> {
    const title = this.personalForm.title.trim();
    if (!title) {
      this.toastr.warning('Dê um título para a missão.', 'Atenção');
      return;
    }

    const request: PersonalMissionRequest = {
      title,
      description: this.personalForm.description.trim() || undefined,
      type: this.personalForm.type,
      actionType: this.personalForm.actionType,
      requiredValue: this.personalForm.requiredValue,
      xpReward: this.personalForm.xpReward
    };

    this.creatingMission = true;
    try {
      await firstValueFrom(this.gamificationService.createPersonalMission(request));
      this.toastr.success('Missão pessoal criada!', 'Sucesso');
      this.showPersonalForm = false;
      this.personalForm = {
        title: '',
        description: '',
        type: 'daily',
        actionType: 'complete_exercises',
        requiredValue: 5,
        xpReward: 25
      };
      await this.loadMissions();
    } catch (err: any) {
      this.toastr.error(this.resolveBackendError(err), 'Erro');
    } finally {
      this.creatingMission = false;
    }
  }

  async deletePersonalMission(mission: Mission, event: Event): Promise<void> {
    event.stopPropagation();
    try {
      await firstValueFrom(this.gamificationService.deletePersonalMission(mission.id));
      this.toastr.success('Missão pessoal removida.', 'Sucesso');
      await this.loadMissions();
    } catch {
      this.toastr.error('Não foi possível remover a missão.', 'Erro');
    }
  }

  private resolveBackendError(err: any): string {
    const errors = err?.error?.errors;
    if (errors && typeof errors === 'object') {
      const first = Object.values(errors)[0];
      if (typeof first === 'string') return first;
    }
    return err?.error?.message ?? 'Não foi possível criar a missão. Verifique os dados e tente novamente.';
  }

  private iconFor(actionType: string): string {
    const map: Record<string, string> = {
      complete_exercises: 'quiz',
      study_minutes: 'schedule',
      complete_trail: 'calculate',
      streak: 'local_fire_department',
      correct_streak: 'emoji_events',
      review_errors: 'refresh'
    };
    return map[actionType] ?? 'flag';
  }

  private categoryFor(actionType: string): string {
    const map: Record<string, string> = {
      complete_exercises: 'Prática',
      study_minutes: 'Tempo',
      complete_trail: 'Trilha',
      streak: 'Streak',
      correct_streak: 'Desafio',
      review_errors: 'Revisão'
    };
    return map[actionType] ?? 'Desafio';
  }

  get completedMissions(): number {
    return this.missions.filter(m => m.done).length;
  }

  get totalXP(): number {
    return this.missions.filter(m => m.done).reduce((sum, m) => sum + m.xp, 0);
  }

  get progressPercentage(): number {
    return this.missions.length === 0 ? 0 : (this.completedMissions / this.missions.length) * 100;
  }

  async toggleMission(mission: Mission): Promise<void> {
    // Missões já reclamadas não podem ser desmarcadas (backend não possui desfazer).
    if (mission.done) {
      return;
    }

    // Só permite reclamar o XP quando a meta foi realmente atingida.
    if (!mission.claimable) {
      this.toastr.warning(
        `Progresso ${mission.progress}/${mission.requiredValue}. Continue estudando!`,
        'Missão em andamento'
      );
      return;
    }

    try {
      const res = await firstValueFrom(this.gamificationService.completeMission(mission.id));
      mission.done = true;
      mission.claimable = false;
      const xpEarned = res?.xpEarned ?? mission.xp;
      this.toastr.success(`Missão concluída! +${xpEarned} XP`, 'Parabéns');
    } catch {
      this.toastr.error('Não foi possível concluir a missão. Tente novamente.', 'Erro');
    }
  }

  getCategoryColor(category: string): string {
    const colors: { [key: string]: string } = {
      'Prática': '#1e3a8a',
      'Tempo': '#0ea5e9',
      'Trilha': '#f59e0b',
      'Streak': '#dc2626',
      'Desafio': '#8b5cf6',
      'Revisão': '#16a34a'
    };
    return colors[category] || '#64748b';
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
    localStorage.setItem('sidebarOpen', this.sidebarOpen.toString());
  }
}
