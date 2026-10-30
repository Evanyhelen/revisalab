import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule
  ],
  templateUrl: './reset-password.html',
  styleUrl: './reset-password.scss',
})
export class ResetPasswordComponent implements OnInit {
  token: string = '';
  novaSenha: string = '';
  confirmarSenha: string = '';
  hidePassword: boolean = true;
  hideConfirmPassword: boolean = true;
  isLoading: boolean = false;
  success: boolean = false;

  submitted: boolean = false;
  senhaError: string = '';
  confirmarSenhaError: string = '';
  requestError: string = '';

  // Token ausente/inválido detectado já na abertura da tela.
  invalidLink: boolean = false;

  constructor(
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
    if (!this.token) {
      this.invalidLink = true;
    }
  }

  onSubmit(): void {
    this.submitted = true;
    this.requestError = '';
    this.validate();

    if (this.senhaError || this.confirmarSenhaError) {
      this.toastr.warning('Verifique os campos destacados.', 'Atenção');
      return;
    }

    if (this.isLoading) return;
    this.isLoading = true;

    this.authService.resetPassword(this.token, this.novaSenha).subscribe({
      next: () => {
        this.isLoading = false;
        this.success = true;
        this.toastr.success('Senha redefinida! Você já pode entrar com a nova senha.', 'Sucesso');
      },
      error: (error) => {
        this.isLoading = false;
        const status = error?.status;
        if (status === 400) {
          // Link inválido, expirado ou já utilizado.
          this.invalidLink = true;
          this.requestError = error?.error?.message || 'Link de recuperação inválido ou expirado.';
        } else if (status === 0) {
          this.requestError = 'Não foi possível conectar ao servidor. Verifique sua conexão.';
        } else if (status >= 500) {
          this.requestError = 'O servidor está temporariamente indisponível. Tente novamente em instantes.';
        } else {
          this.requestError = error?.error?.message || 'Não foi possível redefinir a senha. Tente novamente.';
        }
      }
    });
  }

  onSenhaInput(): void {
    if (this.submitted) {
      this.validateSenha();
      if (this.confirmarSenha) this.validateConfirmar();
    }
  }

  onConfirmarInput(): void {
    if (this.submitted) this.validateConfirmar();
  }

  private validate(): void {
    this.validateSenha();
    this.validateConfirmar();
  }

  private validateSenha(): void {
    if (!this.novaSenha) {
      this.senhaError = 'Nova senha é obrigatória';
    } else if (this.novaSenha.length < 6) {
      this.senhaError = 'A senha deve ter pelo menos 6 caracteres';
    } else {
      this.senhaError = '';
    }
  }

  private validateConfirmar(): void {
    if (!this.confirmarSenha) {
      this.confirmarSenhaError = 'Confirme a nova senha';
    } else if (this.novaSenha !== this.confirmarSenha) {
      this.confirmarSenhaError = 'As senhas não coincidem';
    } else {
      this.confirmarSenhaError = '';
    }
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  toggleConfirmPasswordVisibility(): void {
    this.hideConfirmPassword = !this.hideConfirmPassword;
  }

  navigateToLogin(): void {
    this.router.navigate(['/auth/login']);
  }

  navigateToForgot(): void {
    this.router.navigate(['/auth/forgot-password']);
  }
}
