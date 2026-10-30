import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-sobre',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule
  ],
  templateUrl: './sobre.html',
  styleUrl: './sobre.scss',
})
export class SobreComponent {
  sidebarOpen = true;

  constructor() {
    const savedState = localStorage.getItem('sidebarOpen');
    if (savedState !== null) {
      this.sidebarOpen = savedState === 'true';
    }
  }
  projectInfo = {
    author: 'Evanyhelen Martins Farias',
    advisor: 'Allan Christian Krainski Ferrari',
    course: 'Engenharia de Software',
    institution: 'Universidade',
    year: '2026',
    type: 'Projeto Final de Curso (TCC)'
  };

  features = [
    {
      title: 'Trilhas Estruturadas',
      description: 'Conteúdos organizados por área do ensino médio em sequência progressiva.',
      icon: 'route'
    },
    {
      title: 'Gamificação Educacional',
      description: 'Sistema de XP, níveis, conquistas e ranking para manter a motivação.',
      icon: 'emoji_events'
    },
    {
      title: 'Feedback Imediato',
      description: 'Saiba na hora se acertou, com explicação detalhada de cada questão.',
      icon: 'feedback'
    },
    {
      title: 'Painel de Desempenho',
      description: 'Acompanhe sua evolução com gráficos e identificação de pontos fracos.',
      icon: 'insights'
    },
    {
      title: 'Prática Contínua',
      description: 'Banco de questões para praticar e reforçar o aprendizado.',
      icon: 'fitness_center'
    },
    {
      title: 'Streak Diário',
      description: 'Incentivo à constância com bônus para quem estuda todos os dias.',
      icon: 'local_fire_department'
    }
  ];

  technologies = [
    { name: 'Angular 21', description: 'Framework frontend moderno e reativo' },
    { name: 'TypeScript 5.9', description: 'Linguagem com tipagem forte e recursos avançados' },
    { name: 'Spring Boot 3.4', description: 'Framework backend Java 21 para APIs REST' },
    { name: 'H2 / MySQL 8', description: 'H2 no desenvolvimento; MySQL 8 para produção' },
    { name: 'JWT', description: 'Autenticação segura com tokens' },
    { name: 'Angular Material', description: 'Biblioteca de componentes UI' }
  ];

  objectives = [
    {
      title: 'Objetivo Geral',
      description: 'Desenvolver uma plataforma web gamificada que apoie estudantes universitários na revisão estruturada de conteúdos do ensino médio, contribuindo para o nivelamento de conhecimentos e melhoria do desempenho acadêmico.',
      icon: 'flag'
    },
    {
      title: 'Objetivos Específicos',
      items: [
        'Implementar trilhas de aprendizagem organizadas por disciplinas do ensino médio',
        'Desenvolver sistema de gamificação com XP, níveis, conquistas e ranking',
        'Criar painel de desempenho com identificação de pontos fracos',
        'Integrar assistente inteligente para suporte aos estudos',
        'Validar a efetividade da plataforma através de métricas de engajamento'
      ],
      icon: 'checklist'
    }
  ];

  expectedResults = [
    {
      title: 'Aumento do Engajamento',
      description: 'Espera-se que a gamificação aumente a motivação e o tempo dedicado aos estudos',
      icon: 'trending_up',
      metric: '+40%'
    },
    {
      title: 'Melhoria no Desempenho',
      description: 'Redução de lacunas de conhecimento e melhoria nas notas de disciplinas básicas',
      icon: 'school',
      metric: '+25%'
    },
    {
      title: 'Redução da Evasão',
      description: 'Contribuir para a permanência de estudantes no ensino superior',
      icon: 'group',
      metric: '-15%'
    },
    {
      title: 'Acesso Democrático',
      description: 'Plataforma gratuita e acessível 24/7 para todos os estudantes',
      icon: 'public',
      metric: '100%'
    }
  ];

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
    localStorage.setItem('sidebarOpen', this.sidebarOpen.toString());
  }
}
