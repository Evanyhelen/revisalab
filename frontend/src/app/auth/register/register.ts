import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ToastrService } from 'ngx-toastr';
import { AuthService } from '../../services/auth.service';
import { ViewChild } from '@angular/core';
import { NgForm } from '@angular/forms';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class RegisterComponent {
  @ViewChild('registerForm') registerForm!: NgForm;
  
  nome: string = '';
  email: string = '';
  senha: string = '';
  confirmarSenha: string = '';
  curso: string = '';
  semestre: string = '';
  instituicao: string = '';
  
  hidePassword: boolean = true;
  hideConfirmPassword: boolean = true;
  isLoading: boolean = false;
  
  // Form validation states
  formErrors: { [key: string]: string } = {};
  fieldTouched: { [key: string]: boolean } = {};

  cursos: string[] = [
    'Engenharia de Software',
    'Engenharia Civil',
    'Engenharia Elétrica',
    'Engenharia Mecânica',
    'Ciência da Computação',
    'Sistemas de Informação',
    'Matemática',
    'Física',
    'Química',
    'Biologia',
    'Medicina',
    'Enfermagem',
    'Farmacêutica',
    'Administração',
    'Economia',
    'Direito',
    'Psicologia',
    'Pedagogia',
    'Letras',
    'História',
    'Geografia',
    'Arquitetura e Urbanismo',
    'Design',
    'Outro'
  ];

  semestres: string[] = [
    '1º Semestre',
    '2º Semestre',
    '3º Semestre',
    '4º Semestre',
    '5º Semestre',
    '6º Semestre',
    '7º Semestre',
    '8º Semestre',
    '9º Semestre',
    '10º Semestre'
  ];

  constructor(
    private authService: AuthService,
    private router: Router,
    private toastr: ToastrService
  ) {}

  onFieldBlur(fieldName: string): void {
    this.fieldTouched[fieldName] = true;
    this.validateField(fieldName);
    // Valida também o campo de confirmação de senha quando a senha muda
    if (fieldName === 'senha' && this.fieldTouched['confirmarSenha']) {
      this.validateField('confirmarSenha');
    }
  }

  onFieldInput(fieldName: string): void {
    // Valida em tempo real enquanto digita
    if (this.fieldTouched[fieldName]) {
      this.validateField(fieldName);
    }
    // Valida confirmação de senha em tempo real
    if (fieldName === 'senha' && this.fieldTouched['confirmarSenha']) {
      this.validateField('confirmarSenha');
    }
    // Força revalidação geral do formulário
    this.validateAllFields();
  }

  validateAllFields(): void {
    // Valida todos os campos que foram tocados
    Object.keys(this.fieldTouched).forEach(field => {
      if (this.fieldTouched[field]) {
        this.validateField(field);
      }
    });
  }

  validateField(fieldName: string): void {
    switch (fieldName) {
      case 'nome':
        if (!this.nome || this.nome.trim().length < 3) {
          this.formErrors[fieldName] = 'Nome deve ter pelo menos 3 caracteres';
        } else {
          delete this.formErrors[fieldName];
        }
        break;
      case 'email':
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!this.email) {
          this.formErrors[fieldName] = 'Email é obrigatório';
        } else if (!emailRegex.test(this.email)) {
          this.formErrors[fieldName] = 'Email inválido';
        } else {
          delete this.formErrors[fieldName];
        }
        break;
      case 'senha':
        if (!this.senha) {
          this.formErrors[fieldName] = 'Senha é obrigatória';
        } else if (this.senha.length < 6) {
          this.formErrors[fieldName] = 'Senha deve ter pelo menos 6 caracteres';
        } else {
          delete this.formErrors[fieldName];
        }
        break;
      case 'confirmarSenha':
        if (!this.confirmarSenha) {
          this.formErrors[fieldName] = 'Confirmação de senha é obrigatória';
        } else if (this.senha !== this.confirmarSenha) {
          this.formErrors[fieldName] = 'As senhas não coincidem';
        } else {
          delete this.formErrors[fieldName];
        }
        break;
      case 'curso':
        if (!this.curso) {
          this.formErrors[fieldName] = 'Curso é obrigatório';
        } else {
          delete this.formErrors[fieldName];
        }
        break;
      case 'semestre':
        if (!this.semestre) {
          this.formErrors[fieldName] = 'Semestre é obrigatório';
        } else {
          delete this.formErrors[fieldName];
        }
        break;
      case 'instituicao':
        if (!this.instituicao || this.instituicao.trim().length < 3) {
          this.formErrors[fieldName] = 'Instituição deve ter pelo menos 3 caracteres';
        } else {
          delete this.formErrors[fieldName];
        }
        break;
    }
  }

  isFormValid(): boolean {
    return !!(this.nome && this.email && this.senha && this.confirmarSenha && 
           this.curso && this.semestre && this.instituicao &&
           this.senha === this.confirmarSenha && this.senha.length >= 6 &&
           Object.keys(this.formErrors).length === 0);
  }

  onSubmit(): void {
    // Marca todos os campos como tocados para mostrar validações
    const allFields = ['nome', 'email', 'senha', 'confirmarSenha', 'curso', 'semestre', 'instituicao'];
    allFields.forEach(field => {
      this.fieldTouched[field] = true;
      this.validateField(field);
    });

    if (!this.isFormValid()) {
      this.toastr.warning('Por favor, corrija os erros antes de prosseguir.', 'Atenção');
      return;
    }

    this.isLoading = true;

    this.authService.signup({
      name: this.nome.trim(),
      email: this.email.toLowerCase().trim(),
      password: this.senha,
      curso: this.curso,
      semestre: this.semestre,
      instituicao: this.instituicao.trim()
    }).subscribe({
      next: () => {
        this.isLoading = false;
        this.toastr.success('Cadastro realizado com sucesso!', 'Sucesso');
        this.router.navigate(['/auth/login']);
      },
      error: (error) => {
        this.isLoading = false;
        if (error.status === 409) {
          this.toastr.error('Este email já está cadastrado. Use outro email ou recupere sua senha.', 'Erro');
        } else {
          const message = error?.error?.message || 'Erro ao realizar cadastro. Tente novamente.';
          this.toastr.error(message, 'Erro');
        }
      }
    });
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
}
