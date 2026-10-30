import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-impacto',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatIconModule
  ],
  templateUrl: './impacto.html',
  styleUrl: './impacto.scss',
})
export class ImpactoComponent {
  sidebarOpen = true;

  constructor() {
    const savedState = localStorage.getItem('sidebarOpen');
    if (savedState !== null) {
      this.sidebarOpen = savedState === 'true';
    }
  }
  expectedResults = [
    {
      title: 'Aumento do Engajamento',
      description: 'Espera-se que a gamificação aumente a motivação e o tempo dedicado aos estudos',
      icon: 'trending_up',
      metric: '+40%',
      color: '#0ea5e9'
    },
    {
      title: 'Melhoria no Desempenho',
      description: 'Redução de lacunas de conhecimento e melhoria nas notas de disciplinas básicas',
      icon: 'school',
      metric: '+25%',
      color: '#16a34a'
    },
    {
      title: 'Redução da Evasão',
      description: 'Contribuir para a permanência de estudantes no ensino superior',
      icon: 'group',
      metric: '-15%',
      color: '#f59e0b'
    },
    {
      title: 'Acesso Democrático',
      description: 'Plataforma gratuita e acessível 24/7 para todos os estudantes',
      icon: 'public',
      metric: '100%',
      color: '#8b5cf6'
    }
  ];

  contributions = [
    {
      title: 'Prática',
      description: 'Melhorar o desempenho em disciplinas iniciais, reduzir reprovações e fortalecer a permanência estudantil no ensino superior.',
      icon: 'workspace_premium',
      color: '#1e3a8a'
    },
    {
      title: 'Teórica',
      description: 'Integrar gamificação, aprendizagem apoiada por tecnologia e engenharia de software no desenvolvimento de uma solução empírica.',
      icon: 'psychology',
      color: '#0ea5e9'
    },
    {
      title: 'Tecnológica',
      description: 'Aplicar princípios de análise de requisitos, modelagem, arquitetura de software e desenvolvimento web.',
      icon: 'code',
      color: '#f59e0b'
    },
    {
      title: 'Social',
      description: 'Ampliar o acesso a recursos educacionais de qualidade, contribuindo para a democratização do ensino superior.',
      icon: 'diversity_3',
      color: '#16a34a'
    }
  ];

  justification = {
    academic: 'Ampliar o engajamento acadêmico e apoiar estudantes no enfrentamento de lacunas em conteúdos básicos, oferecendo um ambiente que favoreça a prática sistemática e o acompanhamento do progresso.',
    practical: 'Contribuir para melhorar o desempenho em disciplinas iniciais, reduzir reprovações e fortalecer a permanência estudantil no ensino superior.',
    technological: 'Integrar gamificação, aprendizagem apoiada por tecnologia e engenharia de software no desenvolvimento de uma solução empírica que explore o potencial da gamificação no ensino superior.'
  };

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
    localStorage.setItem('sidebarOpen', this.sidebarOpen.toString());
  }
}
