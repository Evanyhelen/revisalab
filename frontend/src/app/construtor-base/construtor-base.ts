import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { Router, RouterModule } from '@angular/router';
import { BuilderService } from '../services/builder.service';
import { ToastrService } from 'ngx-toastr';

interface Question {
  id: number;
  area: string;
  areaColor: string;
  question: string;
  options: string[];
  /** Índice da alternativa correta (0-based). null = validação no servidor (questões da API). */
  correctAnswer: number | null;
  explanation: string;
}

@Component({
  selector: 'app-construtor-base',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatChipsModule
  ],
  templateUrl: './construtor-base.html',
  styleUrls: ['./construtor-base.scss']
})
export class ConstrutorBaseComponent implements OnInit {
  gameStarted = false;
  currentQuestionIndex = 0;
  correctAnswers = 0;
  totalQuestions = 8;
  selectedAnswer: number | null = null;
  answered = false;
  submitting = false;
  isCorrect = false;
  showCelebration = false;
  buildingHeight = 0;
  sidebarCollapsed = false;
  loading = false;
  loadError = false;
  Math = Math;

  @ViewChild('gameHeading') private gameHeading?: ElementRef;
  @ViewChild('startRetry') private startRetry?: ElementRef;
  @ViewChild('celebrationRetry') private celebrationRetry?: ElementRef;

  questions: Question[] = [];

  /** Momento em que a sessão começou, para rastrear a duração do estudo. */
  private sessionStartedAt = 0;

  // Questões de demonstração (fallback) — usadas apenas quando a API do ENEM
  // estiver indisponível. O fluxo normal consome GET /builder/questions.
  private readonly fallbackQuestions: Question[] = [
    {
      id: 1,
      area: 'MATEMÁTICA',
      areaColor: '#1e3a8a',
      question: 'Quanto é 15 + 27?',
      options: ['40', '42', '44', '48'],
      correctAnswer: 1,
      explanation: '15 + 27 = 42. Somamos as unidades (5+7=12) e as dezenas (1+2=3), resultando em 42.'
    },
    {
      id: 2,
      area: 'MATEMÁTICA',
      areaColor: '#1e3a8a',
      question: 'Qual é a raiz quadrada de 64?',
      options: ['6', '7', '8', '9'],
      correctAnswer: 2,
      explanation: 'A raiz quadrada de 64 é 8, pois 8 × 8 = 64.'
    },
    {
      id: 3,
      area: 'CIÊNCIAS',
      areaColor: '#16a34a',
      question: 'Qual é a fórmula da água?',
      options: ['H2O', 'CO2', 'O2', 'H2SO4'],
      correctAnswer: 0,
      explanation: 'A fórmula da água é H2O: 2 átomos de hidrogênio e 1 de oxigênio.'
    },
    {
      id: 4,
      area: 'PORTUGUÊS',
      areaColor: '#dc2626',
      question: 'Qual é o plural de "cidadão"?',
      options: ['cidadões', 'cidadãos', 'cidadães', 'cidadans'],
      correctAnswer: 1,
      explanation: 'O plural correto de "cidadão" é "cidadãos".'
    },
    {
      id: 5,
      area: 'HISTÓRIA',
      areaColor: '#f59e0b',
      question: 'Em que ano foi proclamada a Independência do Brasil?',
      options: ['1500', '1822', '1889', '1922'],
      correctAnswer: 1,
      explanation: 'A Independência do Brasil foi proclamada em 7 de setembro de 1822.'
    },
    {
      id: 6,
      area: 'GEOGRAFIA',
      areaColor: '#0ea5e9',
      question: 'Qual é o maior país da América do Sul?',
      options: ['Argentina', 'Brasil', 'Peru', 'Colômbia'],
      correctAnswer: 1,
      explanation: 'O Brasil é o maior país da América do Sul em extensão territorial.'
    },
    {
      id: 7,
      area: 'CIÊNCIAS',
      areaColor: '#16a34a',
      question: 'Quantos planetas existem no Sistema Solar?',
      options: ['7', '8', '9', '10'],
      correctAnswer: 1,
      explanation: 'Existem 8 planetas no Sistema Solar: Mercúrio, Vênus, Terra, Marte, Júpiter, Saturno, Urano e Netuno.'
    },
    {
      id: 8,
      area: 'MATEMÁTICA',
      areaColor: '#1e3a8a',
      question: 'Quanto é 12 × 8?',
      options: ['84', '92', '96', '104'],
      correctAnswer: 2,
      explanation: '12 × 8 = 96. Podemos calcular como (10 × 8) + (2 × 8) = 80 + 16 = 96.'
    }
  ];

  constructor(
    private router: Router,
    private builderService: BuilderService,
    private toastr: ToastrService
  ) {}

  ngOnInit(): void {
    // Usa a chave global 'sidebarOpen' (padrão das demais páginas), invertendo
    // para o estado 'collapsed' usado neste template.
    const savedState = localStorage.getItem('sidebarOpen');
    this.sidebarCollapsed = savedState === 'false';
  }

  toggleSidebar(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
    localStorage.setItem('sidebarOpen', String(!this.sidebarCollapsed));
  }

  get currentQuestion(): Question {
    return this.questions[this.currentQuestionIndex];
  }

  get progress(): number {
    return ((this.currentQuestionIndex + 1) / this.totalQuestions) * 100;
  }

  startGame(): void {
    this.loadQuestions();
  }

  /**
   * Busca questões novas na API do Construtor de Base. A cada início/reinício o
   * backend sorteia um ano e disciplinas diferentes — então "jogar de novo" traz
   * perguntas novas. Em caso de falha, exibe erro com opção de tentar novamente
   * (sem iniciar uma sessão silenciosamente com dados de demonstração).
   */
  private loadQuestions(): void {
    // Evita requisições duplicadas em duplo clique ("Construir Novamente").
    if (this.loading) return;
    this.loading = true;
    this.loadError = false;
    this.builderService.getQuestions(8).subscribe({
      next: (apiQuestions) => {
        const mapped = this.mapApiQuestions(apiQuestions);
        // A API respondeu, mas sem perguntas válidas: mantém o material de
        // demonstração para a experiência não travar.
        this.beginSession(mapped.length > 0 ? mapped : this.fallbackQuestions);
      },
      error: () => {
        this.loading = false;
        this.loadError = true;
        // Move o foco para o botão de "tentar novamente" da tela atualmente
        // visível (início ou celebração), mantendo o fluxo acessível.
        setTimeout(() => {
          const target = this.showCelebration ? this.celebrationRetry : this.startRetry;
          target?.nativeElement?.focus();
        }, 0);
      }
    });
  }

  /** Reexecuta a busca de questões após uma falha. */
  retryLoad(): void {
    this.loadQuestions();
  }

  private beginSession(qs: Question[]): void {
    this.questions = qs;
    this.totalQuestions = qs.length;
    this.gameStarted = true;
    this.currentQuestionIndex = 0;
    this.correctAnswers = 0;
    this.selectedAnswer = null;
    this.answered = false;
    this.submitting = false;
    this.isCorrect = false;
    this.buildingHeight = 0;
    this.showCelebration = false;
    this.loading = false;
    this.loadError = false;
    this.sessionStartedAt = Date.now();

    // Anuncia/posiciona o foco na nova atividade assim que o game screen é
    // renderizado, para quem usa teclado ou leitor de tela.
    setTimeout(() => this.gameHeading?.nativeElement?.focus(), 0);
  }

  /**
   * Converte o payload da API do ENEM para o formato interno de questões.
   * O backend devolve: { id, question, options: [{id, text}], correctAnswer (letra), explanation, category, ... }.
   */
  private mapApiQuestions(apiQuestions: any[]): Question[] {
    if (!Array.isArray(apiQuestions)) return [];

    const colorByCategory: Record<string, string> = {
      'Matemática (ENEM)': '#1e3a8a',
      'Ciências da Natureza (ENEM)': '#16a34a',
      'Ciências Humanas (ENEM)': '#f59e0b',
      'Linguagens (ENEM)': '#dc2626'
    };
    const labelByCategory: Record<string, string> = {
      'Matemática (ENEM)': 'MATEMÁTICA',
      'Ciências da Natureza (ENEM)': 'CIÊNCIAS',
      'Ciências Humanas (ENEM)': 'HUMANAS',
      'Linguagens (ENEM)': 'LINGUAGENS'
    };

    return apiQuestions
      .map((q, idx) => {
        const options = Array.isArray(q?.options) ? q.options : [];
        const texts: string[] = options.map((o: any) => o?.text ?? '');
        const category = q?.category ?? '';
        return {
          id: q?.id ?? idx + 1,
          area: labelByCategory[category] ?? 'ENEM',
          areaColor: colorByCategory[category] ?? '#6b7280',
          question: q?.question ?? '',
          options: texts,
          // A resposta correta não vem na listagem; é resolvida no servidor via
          // POST /builder/submit. A explicação também chega só após a tentativa.
          correctAnswer: null,
          explanation: ''
        };
      })
      .filter((q: Question) => q.question && q.options.length >= 2);
  }

  selectAnswer(index: number): void {
    if (this.answered || this.submitting) return;
    this.selectedAnswer = index;
  }

  submitAnswer(): void {
    if (this.selectedAnswer === null || this.answered || this.submitting) return;

    const chosenIndex = this.selectedAnswer;
    const q = this.currentQuestion;
    const correct = q.correctAnswer;

    if (correct != null) {
      // Questão de demonstração (fallback local): validação no cliente.
      this.answered = true;
      this.isCorrect = chosenIndex === correct;
      this.applyAnswerResult();
      return;
    }

    // Questão da API: valida no servidor para não expor o gabarito no cliente.
    // 'submitting' mantém a tela travada até o resultado chegar, evitando que o
    // usuário avance e o resultado seja aplicado à questão errada.
    this.submitting = true;
    const letter = 'ABCDE'[chosenIndex];
    this.builderService.submitAnswer(q.id, letter).subscribe({
      next: (res) => {
        this.submitting = false;
        this.answered = true;
        this.isCorrect = res?.correct === true;
        q.correctAnswer = this.letterToIndex(res?.correctAnswer);
        q.explanation = res?.explanation ?? '';
        this.applyAnswerResult();
      },
      error: () => {
        this.submitting = false;
        this.answered = true;
        this.isCorrect = false;
        this.applyAnswerResult();
        this.toastr.warning('Não foi possível validar a resposta no servidor.', 'Atenção');
      }
    });
  }

  /** Aplica o resultado (acerto/erro) ao progresso visual da base. */
  private applyAnswerResult(): void {
    if (this.isCorrect) {
      this.correctAnswers++;
      this.buildingHeight++;
      this.animateBlockAdd();
    } else if (this.buildingHeight > 0) {
      // Regra "errou → perde um bloco" (nunca abaixo de zero).
      this.buildingHeight--;
    }
  }

  private letterToIndex(letter?: string | null): number {
    if (letter == null) return 0;
    const idx = 'ABCDE'.indexOf(letter.toUpperCase());
    return idx >= 0 ? idx : 0;
  }

  nextQuestion(): void {
    // Prevent double-click by checking if already showing celebration
    if (this.showCelebration) return;

    if (this.currentQuestionIndex < this.totalQuestions - 1) {
      this.currentQuestionIndex++;
      this.selectedAnswer = null;
      this.answered = false;
      this.isCorrect = false;
    } else {
      // Last question - show celebration immediately
      this.playVictorySound();
      this.showCelebration = true;
      this.persistSessionXp();
      setTimeout(() => {
        this.playCelebrationSound();
      }, 800);
    }
  }

  /**
   * Persiste o XP da sessão no backend ao concluir as 8 questões.
   * A validação das respostas permanece local; o backend só registra o total de acertos.
   */
  private persistSessionXp(): void {
    const durationSeconds = Math.max(0, Math.round((Date.now() - this.sessionStartedAt) / 1000));
    this.builderService.completeSession(this.correctAnswers, this.totalQuestions, durationSeconds).subscribe({
      next: (res) => {
        // 15 XP por acerto é a regra do backend (XP_PER_CORRECT_BLOCK); o
        // fallback local espelha esse valor para não exibir XP incorreto.
        const xp = res?.xpEarned ?? this.correctAnswers * 15;
        this.toastr.success(`Sessão concluída! +${xp} XP adicionados ao seu perfil.`, 'XP registrado');
      },
      error: () => {
        this.toastr.warning('Não foi possível registrar o XP no servidor.', 'Atenção');
      }
    });
  }

  playVictorySound(): void {
    try {
      // Create a victory fanfare sound
      const audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      const notes = [
        { freq: 523.25, duration: 0.15 }, // C
        { freq: 659.25, duration: 0.15 }, // E
        { freq: 783.99, duration: 0.15 }, // G
        { freq: 1046.50, duration: 0.4 }  // C (high)
      ];

      let startTime = audioContext.currentTime;

      notes.forEach((note, index) => {
        const oscillator = audioContext.createOscillator();
        const gainNode = audioContext.createGain();

        oscillator.connect(gainNode);
        gainNode.connect(audioContext.destination);

        oscillator.frequency.value = note.freq;
        oscillator.type = 'triangle';

        gainNode.gain.setValueAtTime(0.4, startTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, startTime + note.duration);

        oscillator.start(startTime);
        oscillator.stop(startTime + note.duration);

        startTime += note.duration;
      });
    } catch (e) {
      // Áudio é um extra; nunca deve bloquear a conclusão da sessão.
    }
  }

  animateBlockAdd(): void {
    const blocks = document.querySelectorAll('.building-block');
    if (blocks.length > 0) {
      const lastBlock = blocks[blocks.length - 1] as HTMLElement;
      lastBlock.classList.add('block-added');
    }
  }

  playCelebrationSound(): void {
    try {
      // Create a simple celebration sound using Web Audio API
      const audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      const notes = [523.25, 587.33, 659.25, 783.99, 880.00]; // C, D, E, G, A

      notes.forEach((freq, index) => {
        const oscillator = audioContext.createOscillator();
        const gainNode = audioContext.createGain();

        oscillator.connect(gainNode);
        gainNode.connect(audioContext.destination);

        oscillator.frequency.value = freq;
        oscillator.type = 'sine';

        const startTime = audioContext.currentTime + (index * 0.15);
        gainNode.gain.setValueAtTime(0.3, startTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, startTime + 0.3);

        oscillator.start(startTime);
        oscillator.stop(startTime + 0.3);
      });
    } catch (e) {
      // Áudio é um extra; nunca deve bloquear a conclusão da sessão.
    }
  }

  restartGame(): void {
    if (this.loading) return;
    this.startGame();
  }

  goToFundamentacao(): void {
    this.router.navigate(['/fundamentacao']);
  }
}
