import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { firstValueFrom } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule
  ],
  templateUrl: './perfil.html',
  styleUrl: './perfil.scss'
})
export class PerfilComponent implements OnInit {
  editMode = false;
  saving = false;

  // Opções de avatar que o usuário pode alternar ao clicar no lápis.
  private readonly avatars = ['👩‍🎓', '👨‍🎓', '🧑‍🎓', '🦉', '📚', '🚀', '🧠', '💡'];

  student = {
    name: '',
    university: '',
    course: '',
    semester: '',
    email: '',
    avatar: '👩‍🎓',
    xp: 0,
    level: 1,
    streak: 0
  };

  constructor(
    private authService: AuthService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.loadStudent();
  }

  private loadStudent(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.student = {
        name: user.name ?? '',
        university: user.instituicao ?? '',
        course: user.curso ?? '',
        semester: user.semestre ?? '',
        email: user.email ?? '',
        avatar: this.authService.getAvatar() || '👩‍🎓',
        xp: user.xp ?? 0,
        level: user.level ?? 1,
        streak: user.streak ?? 0
      };
    }
  }

  /** Alterna para o próximo avatar da lista (botão de lápis do avatar). */
  changeAvatar(): void {
    const currentIndex = this.avatars.indexOf(this.student.avatar);
    // Um valor fora da lista (ex.: 🎓 padrão) cairia em -1 e voltaria para o
    // índice 0; partimos de -1 para que o próximo seja o primeiro da lista.
    const nextIndex = (currentIndex + 1) % this.avatars.length;
    this.student.avatar = this.avatars[nextIndex];
    // Persiste a escolha para que o avatar não volte ao padrão ao salvar/recarregar.
    this.authService.updateAvatar(this.student.avatar);
  }

  async saveProfile(): Promise<void> {
    if (this.saving) return;
    this.saving = true;
    try {
      await firstValueFrom(this.authService.updateProfile({
        name: this.student.name,
        curso: this.student.course,
        semestre: this.student.semester,
        instituicao: this.student.university
      }));
      this.loadStudent();
      this.editMode = false;
      this.toastr.success('Perfil atualizado com sucesso!', 'Sucesso');
    } catch {
      this.toastr.error('Não foi possível salvar o perfil. Tente novamente.', 'Erro');
    } finally {
      this.saving = false;
    }
  }
}
