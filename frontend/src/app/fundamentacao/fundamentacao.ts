import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-fundamentacao',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatIconModule
  ],
  templateUrl: './fundamentacao.html',
  styleUrl: './fundamentacao.scss',
})
export class FundamentacaoComponent {
  sidebarOpen = true;

  constructor() {
    const savedState = localStorage.getItem('sidebarOpen');
    if (savedState !== null) {
      this.sidebarOpen = savedState === 'true';
    }
  }
  theoreticalPillars = [
    {
      title: 'Evasão e Desempenho Acadêmico',
      description: 'A evasão no ensino superior está associada a dificuldades em disciplinas iniciais, especialmente em conteúdos básicos. Estudos indicam que o desempenho nas primeiras disciplinas influencia a permanência e o sucesso acadêmico.',
      icon: 'school',
      color: '#1e3a8a',
      reference: 'Tinto (1993)'
    },
    {
      title: 'Lacunas de Aprendizagem',
      description: 'Muitos estudantes ingressam na universidade com defasagens em conteúdos do ensino médio, comprometendo o acompanhamento das disciplinas e aumentando a probabilidade de reprovações sucessivas.',
      icon: 'trending_down',
      color: '#dc2626',
      reference: 'Literatura sobre evasão'
    },
    {
      title: 'Gamificação na Educação',
      description: 'O uso de elementos de jogos em contextos educacionais aumenta engajamento, motivação e persistência. Pontos, níveis, conquistas e feedback imediato tornam o estudo mais atrativo.',
      icon: 'emoji_events',
      color: '#f59e0b',
      reference: 'Deterding et al. (2011); Kapp (2012)'
    },
    {
      title: 'Plataformas Digitais de Estudo',
      description: 'Ambientes virtuais que integram conteúdos, atividades e feedback em trilhas estruturadas permitem organizar a revisão de forma dinâmica, com acompanhamento do progresso.',
      icon: 'devices',
      color: '#0ea5e9',
      reference: 'Domínguez et al. (2013)'
    }
  ];

  researchContext = {
    problem: 'A evasão e o baixo aproveitamento em disciplinas iniciais do ensino superior estão associados a lacunas de aprendizagem provenientes do ensino médio, especialmente em conteúdos fundamentais.',
    question: 'De que forma uma plataforma web gamificada pode apoiar a revisão estruturada de conteúdos do ensino médio, contribuindo para o nivelamento de conhecimentos e para a melhoria do desempenho em disciplinas iniciais do ensino superior?',
    approach: 'Desenvolvimento de uma plataforma web gamificada que integra recursos educacionais em trilhas de aprendizagem estruturadas, com elementos de jogos que incentivem a prática contínua e o acompanhamento do progresso.'
  };

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
    localStorage.setItem('sidebarOpen', this.sidebarOpen.toString());
  }
}
