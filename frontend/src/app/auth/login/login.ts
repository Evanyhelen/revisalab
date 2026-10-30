import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginComponent implements OnInit {
  email: string = '';
  senha: string = '';
  hidePassword: boolean = true;
  isLoading: boolean = false;
  rememberEmail: boolean = false;
  private static readonly REMEMBER_KEY = 'revisalab_remembered_email';

  // Validação visual (borda vermelha + mensagem), igual ao cadastro.
  submitted: boolean = false;
  emailError: string = '';
  senhaError: string = '';

  // Mensagem de erro de autenticação (credenciais/servidor/conexão), exibida
  // inline de forma acessível; o e-mail digitado é preservado para nova tentativa.
  loginError: string = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private toastr: ToastrService
  ) {
    // Garante que o estado de carregamento seja resetado ao entrar na tela
    this.isLoading = false;

    // Restaura o e-mail lembrado ("Lembrar de mim").
    const remembered = localStorage.getItem(LoginComponent.REMEMBER_KEY);
    if (remembered) {
      this.email = remembered;
      this.rememberEmail = true;
    }
  }

  ngOnInit(): void {
    // O interceptor redireciona para /auth/login?sessionExpired=true quando um
    // 401 é recebido; aqui convertemos esse flag em um aviso claro para o usuário.
    if (this.route.snapshot.queryParamMap.get('sessionExpired') === 'true') {
      this.toastr.info('Sua sessão expirou. Faça login novamente para continuar.', 'Sessão expirada');
    }
  }

  onSubmit(): void {
    // Evita submissões duplicadas (clique duplo ou Enter repetido).
    if (this.isLoading) return;

    this.submitted = true;
    this.loginError = '';
    this.validate();

    if (this.emailError || this.senhaError) {
      this.toastr.warning('Por favor, preencha os campos corretamente.', 'Atenção');
      return;
    }

    this.isLoading = true;

    this.authService.login({
      email: this.email.toLowerCase().trim(),
      password: this.senha
    }).subscribe({
      next: (response) => {
        // "Lembrar de mim": grava (ou limpa) o e-mail no dispositivo.
        if (this.rememberEmail) {
          localStorage.setItem(LoginComponent.REMEMBER_KEY, response.email);
        } else {
          localStorage.removeItem(LoginComponent.REMEMBER_KEY);
        }

        // Toast antes de resetar o loading: ngx-toastr dispara um tick síncrono
        // que, caso o estado já tivesse mudado, causaria NG0100.
        this.toastr.success(`Bem-vindo(a), ${response.name}!`, 'Login realizado');
        this.isLoading = false;
        // Respeita a rota de origem quando o guard redirecionou para cá; senão vai ao dashboard.
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigate([returnUrl || '/dashboard']);
      },
      error: (error) => {
        this.isLoading = false;
        this.loginError = this.resolveLoginError(error);
        this.toastr.error(this.loginError, 'Erro de login');
      }
    });
  }

  /**
   * Diferencia os três cenários de falha do login:
   * - credenciais inválidas (400/401 do backend);
   * - servidor indisponível (5xx);
   * - falha de conexão (status 0, sem resposta HTTP).
   */
  private resolveLoginError(error: any): string {
    const status = error?.status;
    if (status === 0) {
      return 'Não foi possível conectar ao servidor. Verifique sua conexão com a internet.';
    }
    if (status >= 500) {
      return 'O servidor está temporariamente indisponível. Tente novamente em instantes.';
    }
    // 400/401: credenciais inválidas. Prioriza a mensagem do backend e, na falta
    // dela, mantém o texto padrão exigido.
    return error?.error?.message || 'Email ou senha incorretos.';
  }

  onEmailInput(): void {
    if (this.submitted) this.validateEmail();
  }

  onSenhaInput(): void {
    if (this.submitted) this.validateSenha();
  }

  private validate(): void {
    this.validateEmail();
    this.validateSenha();
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

  private validateSenha(): void {
    this.senhaError = !this.senha ? 'Senha é obrigatória' : '';
  }

  togglePasswordVisibility(): void {
    this.hidePassword = !this.hidePassword;
  }

  navigateToRegister(): void {
    this.router.navigate(['/auth/register']);
  }

  onForgotPassword(): void {
    this.router.navigate(['/auth/forgot-password']);
  }
}
