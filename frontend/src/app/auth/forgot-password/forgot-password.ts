import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule
  ],
  templateUrl: './forgot-password.html',
  styleUrl: './forgot-password.scss',
})
export class ForgotPasswordComponent {
  email: string = '';
  isLoading: boolean = false;
  sent: boolean = false;

  // Validação visual (mesmo padrão do login).
  submitted: boolean = false;
  emailError: string = '';

  // Mensagem de erro acessível (conexão/servidor), exibida inline.
  requestError: string = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService
  ) {}

  onSubmit(): void {
    this.submitted = true;
    this.requestError = '';
    this.validateEmail();

    if (this.emailError) {
      this.toastr.warning('Informe um e-mail válido.', 'Atenção');
      return;
    }

    if (this.isLoading) return;
    this.isLoading = true;

    this.authService.forgotPassword(this.email.toLowerCase().trim()).subscribe({
      next: () => {
        this.isLoading = false;
        // A mensagem é a mesma exista ou não a conta — não revelamos o cadastro.
        this.sent = true;
      },
      error: (error) => {
        this.isLoading = false;
        this.requestError = this.resolveError(error);
      }
    });
  }

  onEmailInput(): void {
    if (this.submitted) this.validateEmail();
  }

  private validateEmail(): void {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!this.email.trim()) {
      this.emailError = 'Email é obrigatório';
    } else if (!emailRegex.test(this.email.trim())) {
      this.emailError = 'Email inválido';
    } else {
      this.emailError = '';
    }
  }

  private resolveError(error: any): string {
    const status = error?.status;
    if (status === 0) {
      return 'Não foi possível conectar ao servidor. Verifique sua conexão com a internet.';
    }
    if (status >= 500) {
      return 'O servidor está temporariamente indisponível. Tente novamente em instantes.';
    }
    return error?.error?.message || 'Não foi possível enviar a solicitação. Tente novamente.';
  }

  navigateToLogin(): void {
    this.router.navigate(['/auth/login']);
  }
}
