import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FeedbackService } from '../services/feedback.service';

@Component({
  selector: 'app-feedback',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule
  ],
  templateUrl: './feedback.html',
  styleUrl: './feedback.scss',
})
export class Feedback {
  rating = 0;
  comment = '';
  hoveredRating = 0;
  isSubmitting = false;

  constructor(
    private router: Router,
    private snackBar: MatSnackBar,
    private feedbackService: FeedbackService
  ) {}

  setRating(rating: number): void {
    this.rating = rating;
  }

  setHoveredRating(rating: number): void {
    this.hoveredRating = rating;
  }

  clearHoveredRating(): void {
    this.hoveredRating = 0;
  }

  getStarIcon(index: number): string {
    const currentRating = this.hoveredRating || this.rating;
    return index <= currentRating ? 'star' : 'star_outline';
  }

  submitFeedback(): void {
    if (this.rating === 0) {
      this.snackBar.open('Por favor, avalie a plataforma antes de enviar.', 'Fechar', {
        duration: 3000,
        panelClass: ['warning-snackbar']
      });
      return;
    }

    // Impede duplo clique de disparar múltiplos POSTs (feedback duplicado).
    if (this.isSubmitting) return;
    this.isSubmitting = true;

    // Envia o feedback para o backend (persistência real).
    this.feedbackService.submitFeedback(this.rating, this.comment).subscribe({
      next: () => {
        this.snackBar.open('Obrigado pelo seu feedback! Ele nos ajuda a melhorar.', 'Fechar', {
          duration: 4000,
          panelClass: ['success-snackbar']
        });

        // Limpar formulário
        this.rating = 0;
        this.comment = '';
        this.isSubmitting = false;

        // Redirecionar para o dashboard (o snackbar global permanece visível
        // durante a navegação, então não é preciso esperar aqui).
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.isSubmitting = false;
        this.snackBar.open('Não foi possível enviar o feedback. Tente novamente.', 'Fechar', {
          duration: 4000,
          panelClass: ['warning-snackbar']
        });
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/dashboard']);
  }
}