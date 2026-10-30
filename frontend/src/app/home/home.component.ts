import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {
  features = [
    {
      icon: '🎯',
      title: 'Trilhas Estruturadas',
      desc: 'Percursos organizados por área: Matemática, Português, Física, Química e Raciocínio Lógico.'
    },
    {
      icon: '⚡',
      title: 'Feedback Imediato',
      desc: 'Saiba na hora se acertou ou errou, com explicação detalhada e link para aprofundar.'
    },
    {
      icon: '🏆',
      title: 'Sistema de XP e Níveis',
      desc: 'Cada questão respondida e lição concluída gera XP. Suba de nível e desbloqueie conquistas.'
    },
    {
      icon: '📊',
      title: 'Painel de Desempenho',
      desc: 'Acompanhe sua evolução com gráficos, taxa de acerto por área e pontos fracos identificados.'
    }
  ];

  steps = [
    {
      num: '01',
      icon: '🗺️',
      title: 'Escolha sua Trilha',
      desc: 'Selecione a área que precisa reforçar e comece pelo módulo inicial.'
    },
    {
      num: '02',
      icon: '🎮',
      title: 'Pratique e Ganhe XP',
      desc: 'Responda questões, complete lições e acumule pontos de experiência.'
    },
    {
      num: '03',
      icon: '📈',
      title: 'Evolua e Suba no Ranking',
      desc: 'Acompanhe seu progresso, conquiste badges e compete no ranking semanal.'
    }
  ];

  testimonials = [
    {
      emoji: '👩',
      name: 'Maria Eduarda Silva',
      uni: 'USP — Medicina',
      year: 'Ingressou em 2024',
      stars: 5,
      text: '"O RevisaLab me ajudou a nivelar minha base de Biologia e Química antes de entrar na graduação. Os exercícios com feedback imediato foram essenciais para identificar onde eu errava."'
    },
    {
      emoji: '👨',
      name: 'João Pedro Santos',
      uni: 'UNICAMP — Engenharia',
      year: 'Ingressou em 2024',
      stars: 5,
      text: '"Tinha muita dificuldade em Matemática básica e as trilhas me ajudaram a organizar o estudo. O sistema de XP me motivou a estudar todo dia!"'
    },
    {
      emoji: '👩',
      name: 'Ana Carolina Lima',
      uni: 'USP — Direito',
      year: 'Ingressou em 2023',
      stars: 5,
      text: '"A plataforma organiza tudo que eu precisava estudar. Sem o RevisaLab eu não saberia por onde começar. A gamificação me manteve constante nos estudos."'
    },
    {
      emoji: '👨',
      name: 'Lucas Oliveira',
      uni: 'UNICAMP — Ciências Sociais',
      year: 'Ingressou em 2024',
      stars: 5,
      text: '"As trilhas são bem estruturadas e o feedback é excelente. Consigo estudar de forma mais organizada e ver minha evolução claramente no painel de desempenho."'
    },
    {
      emoji: '👩',
      name: 'Beatriz Ferreira',
      uni: 'FUVEST — Jornalismo',
      year: 'Ingressou em 2023',
      stars: 5,
      text: '"Comecei com dificuldades em Interpretação de Texto e, com a trilha de Português do RevisaLab, melhorei muito meu desempenho em apenas 3 meses de prática."'
    },
    {
      emoji: '👨',
      name: 'Rafael Costa',
      uni: 'UNESP — Arquitetura',
      year: 'Ingressou em 2024',
      stars: 5,
      text: '"O diferencial é ter autonomia para estudar no seu ritmo com suporte sempre que precisar. O Ranking semanal me faz querer estudar mais para subir de posição!"'
    }
  ];

  areas = [
    { emoji: '📐', name: 'Matemática', count: '180+ questões', color: '#1a2980' },
    { emoji: '📖', name: 'Português', count: '120+ questões', color: '#26d0ce' },
    { emoji: '⚗️', name: 'Química', count: '90+ questões', color: '#8b5cf6' },
    { emoji: '🔭', name: 'Física', count: '80+ questões', color: '#27ae60' },
    { emoji: '🧠', name: 'Raciocínio Lógico', count: '60+ questões', color: '#f5a623' },
  ];

  getStars(n: number): number[] {
    return Array(n).fill(0);
  }
}
