import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { ExerciseService, Exercise } from '../services/exercise.service';

/** Alternativa de uma questão (id = letra A, B, C, D...). */
interface QuizOption { id: string; text: string; correct: boolean; }

/** Questão interna do quiz, unificando exercícios reais (backend) e simulados (fallback). */
interface QuizQuestion {
  exerciseId: number | null;   // null = questão simulada (fallback)
  area: string;
  areaIcon: string;
  difficulty: string;          // 'easy' | 'medium' | 'hard'
  text: string;
  options: QuizOption[];
  explanation: string;
  xp: number;
  correctAnswer: string | null; // letra correta (somente exercícios reais)
}

/** Questão simulada (banco de fallback). */
interface MockQuestion {
  area: string;
  areaIcon: string;
  difficulty: string;
  text: string;
  options: { text: string; correct: boolean }[];
  explanation: string;
  xp: number;
}

/** Cartão de disciplina (área) exibido no carrossel de seleção. */
interface AreaOption {
  id: string;
  icon: string;
  name: string;
  color: string;
  /** Presente apenas nas áreas de ENEM; identifica a disciplina da API externa. */
  enem?: string;
}

@Component({
  selector: 'app-quiz',
  standalone: false,
  templateUrl: './quiz.html',
  styleUrl: './quiz.scss'
})
export class Quiz implements OnInit {
  sidebarOpen = true;

  constructor(
    private exerciseService: ExerciseService,
    private toastr: ToastrService,
    private route: ActivatedRoute
  ) {
    const savedState = localStorage.getItem('sidebarOpen');
    if (savedState !== null) {
      this.sidebarOpen = savedState === 'true';
    }
  }

  ngOnInit(): void {
    // Vindo de "Iniciar Trilha" (ex.: /praticar?area=enem-matematica&cat=Matemática),
    // pré-seleciona a disciplina correspondente sem alterar o layout da página.
    const areaParam = this.route.snapshot.queryParamMap.get('area');
    if (areaParam && this.areas.some(a => a.id === areaParam)) {
      this.selectArea(areaParam);
    }

    // `cat` vincula a sessão à trilha de origem para creditar seu progresso.
    const catParam = this.route.snapshot.queryParamMap.get('cat');
    if (catParam) {
      this.trailCategory = catParam;
    }
  }

  areas: AreaOption[] = [
    { id: 'matematica', icon: 'calculate', name: 'Matemática', color: '#1e3a8a' },
    { id: 'portugues', icon: 'menu_book', name: 'Português', color: '#0ea5e9' },
    { id: 'fisica', icon: 'science', name: 'Física', color: '#8b5cf6' },
    { id: 'quimica', icon: 'biotech', name: 'Química', color: '#16a34a' },
    { id: 'logica', icon: 'psychology', name: 'Raciocínio Lógico', color: '#f59e0b' },
    { id: 'historia', icon: 'history_edu', name: 'História', color: '#dc2626' },
    { id: 'geografia', icon: 'public', name: 'Geografia', color: '#0891b2' },
    { id: 'biologia', icon: 'eco', name: 'Biologia', color: '#65a30d' },
    { id: 'enem-matematica', icon: 'school', name: 'ENEM · Matemática', color: '#0f766e', enem: 'matematica' },
    { id: 'enem-natureza', icon: 'science', name: 'ENEM · Natureza', color: '#166534', enem: 'ciencias-natureza' },
    { id: 'enem-humanas', icon: 'history_edu', name: 'ENEM · Humanas', color: '#7c2d12', enem: 'ciencias-humanas' },
    { id: 'enem-linguagens', icon: 'menu_book', name: 'ENEM · Linguagens', color: '#0e7490', enem: 'linguagens' }
  ];

  /** Mapeia o id da área (UI) para a categoria usada no backend. */
  private readonly areaToCategory: Record<string, string> = {
    'matematica': 'Matemática',
    'portugues': 'Português',
    'fisica': 'Física',
    'quimica': 'Química',
    'logica': 'Raciocínio Lógico',
    'historia': 'História',
    'geografia': 'Geografia',
    'biologia': 'Biologia'
  };

  private readonly areaToEnemDiscipline: Record<string, string> = {
    'enem-matematica': 'matematica',
    'enem-natureza': 'ciencias-natureza',
    'enem-humanas': 'ciencias-humanas',
    'enem-linguagens': 'linguagens'
  };

  /**
   * Disciplina do ENEM equivalente a cada área base (usada como complemento quando
   * o banco local + simulado não possui questões suficientes para atingir `qty`).
   * Espelha o mapeamento de AREA_TO_ENEM em aulas.ts.
   */
  private readonly areaToEnem: Record<string, string> = {
    'matematica': 'matematica',
    'portugues': 'linguagens',
    'fisica': 'ciencias-natureza',
    'quimica': 'ciencias-natureza',
    'logica': 'matematica',
    'historia': 'ciencias-humanas',
    'geografia': 'ciencias-humanas',
    'biologia': 'ciencias-natureza'
  };

  private readonly categoryIcons: Record<string, string> = {
    'Matemática': '📐',
    'Português': '📖',
    'Física': '🔭',
    'Química': '⚗️',
    'Raciocínio Lógico': '🧠',
    'História': '🏛️',
    'Geografia': '🌎',
    'Biologia': '🧬'
  };

  filteredAreas = [...this.areas];
  searchTerm = '';
  carouselIndex = 0;
  carouselItemWidth = 200;
  carouselItemsVisible = 4;

  selectedArea = '';
  difficulty = 'medium';
  qty = 10;

  sessionActive = false;
  sessionFinished = false;
  loading = false;
  loadingMessage = '';
  submitting = false;
  currentIndex = 0;
  totalQuestions = 0;
  selectedOption: number | null = null;
  answered = false;
  lastCorrect = false;
  lastXpEarned = 0;
  feedbackMessage = '';
  earnedXp = 0;
  correctAnswers = 0;
  combo = 0;
  maxCombo = 0;
  sessionQuestions: QuizQuestion[] = [];

  /** XP acumulado apenas nas questões simuladas/ENEM (não persistidas por /submit). */
  private mockXpAccumulated = 0;

  /** Acertos apenas nas questões simuladas/ENEM, para reportar a sessão sem duplicar as reais. */
  private mockCorrectAnswers = 0;

  /** Categoria da trilha de origem (quando a prática veio de "Começar a praticar"). */
  private trailCategory: string | null = null;

  /** Marcadores de tempo para rastrear o estudo (segundos por questão e duração da sessão). */
  private sessionStartedAt = 0;
  private questionStartedAt = 0;

  readonly allQuestions: MockQuestion[] = [
    {
      area: 'Matemática', areaIcon: '📐', difficulty: 'easy',
      text: 'Quanto é 2³ + 4²?',
      options: [
        { text: '12', correct: false },
        { text: '24', correct: true },
        { text: '32', correct: false },
        { text: '48', correct: false }
      ],
      explanation: '2³ = 8 e 4² = 16. Portanto 8 + 16 = 24.',
      xp: 5
    },
    {
      area: 'Matemática', areaIcon: '📐', difficulty: 'medium',
      text: 'Qual é a solução da equação 2x + 6 = 14?',
      options: [
        { text: 'x = 3', correct: false },
        { text: 'x = 4', correct: true },
        { text: 'x = 5', correct: false },
        { text: 'x = 10', correct: false }
      ],
      explanation: '2x = 14 - 6 = 8, então x = 4.',
      xp: 5
    },
    {
      area: 'Matemática', areaIcon: '📐', difficulty: 'medium',
      text: 'O produto de dois números é 48 e a soma é 14. Quais são os números?',
      options: [
        { text: '6 e 8', correct: true },
        { text: '4 e 10', correct: false },
        { text: '3 e 11', correct: false },
        { text: '7 e 7', correct: false }
      ],
      explanation: '6 × 8 = 48 e 6 + 8 = 14. Sistema resolvido por equações do 2º grau.',
      xp: 5
    },
    {
      area: 'Português', areaIcon: '📖', difficulty: 'easy',
      text: 'Qual é a função do conectivo "embora" em uma frase?',
      options: [
        { text: 'Indicar adição', correct: false },
        { text: 'Indicar concessão', correct: true },
        { text: 'Indicar causa', correct: false },
        { text: 'Indicar conclusão', correct: false }
      ],
      explanation: '"Embora" é uma conjunção concessiva, indicando contraste ou concessão.',
      xp: 5
    },
    {
      area: 'Português', areaIcon: '📖', difficulty: 'medium',
      text: 'Em qual alternativa há erro de concordância verbal?',
      options: [
        { text: '"Os alunos estudaram muito"', correct: false },
        { text: '"Fazem dez anos que me formei"', correct: true },
        { text: '"A maioria dos estudantes aprovaram"', correct: false },
        { text: '"Mais de um candidato foi aprovado"', correct: false }
      ],
      explanation: 'Com verbos impessoais como "fazer" indicando tempo, o verbo fica no singular: "Faz dez anos...".',
      xp: 5
    },
    {
      area: 'Física', areaIcon: '🔭', difficulty: 'medium',
      text: 'Um carro percorre 180 km em 2 horas. Qual é sua velocidade média?',
      options: [
        { text: '60 km/h', correct: false },
        { text: '90 km/h', correct: true },
        { text: '120 km/h', correct: false },
        { text: '45 km/h', correct: false }
      ],
      explanation: 'V = d/t = 180/2 = 90 km/h.',
      xp: 5
    },
    {
      area: 'Química', areaIcon: '⚗️', difficulty: 'easy',
      text: 'Qual é o número atômico do oxigênio?',
      options: [
        { text: '6', correct: false },
        { text: '7', correct: false },
        { text: '8', correct: true },
        { text: '16', correct: false }
      ],
      explanation: 'O oxigênio (O) tem número atômico 8, ou seja, possui 8 prótons em seu núcleo.',
      xp: 5
    },
    {
      area: 'Raciocínio Lógico', areaIcon: '🧠', difficulty: 'medium',
      text: 'Se todos os A são B e todos os B são C, então:',
      options: [
        { text: 'Todos os C são A', correct: false },
        { text: 'Alguns C são A', correct: false },
        { text: 'Todos os A são C', correct: true },
        { text: 'Nenhum C é A', correct: false }
      ],
      explanation: 'Por transitividade: A ⊆ B e B ⊆ C implica A ⊆ C. Logo, todos os A são C.',
      xp: 5
    },
    {
      area: 'Matemática', areaIcon: '📐', difficulty: 'hard',
      text: 'Qual é o valor de log₂(64)?',
      options: [
        { text: '5', correct: false },
        { text: '6', correct: true },
        { text: '7', correct: false },
        { text: '8', correct: false }
      ],
      explanation: '2⁶ = 64, portanto log₂(64) = 6.',
      xp: 8
    },
    {
      area: 'Física', areaIcon: '🔭', difficulty: 'hard',
      text: 'Um objeto de 2 kg em queda livre (g = 10 m/s²) percorre quantos metros nos primeiros 3 s?',
      options: [
        { text: '30 m', correct: false },
        { text: '45 m', correct: true },
        { text: '60 m', correct: false },
        { text: '15 m', correct: false }
      ],
      explanation: 'h = ½gt² = ½ × 10 × 9 = 45 m. A massa não influencia a queda livre.',
      xp: 8
    }
  ];

  get currentQuestion(): QuizQuestion | null {
    return this.sessionQuestions[this.currentIndex] ?? null;
  }

  get accuracy(): number {
    if (this.totalQuestions === 0) return 0;
    return Math.round((this.correctAnswers / this.totalQuestions) * 100);
  }

  selectArea(id: string) { this.selectedArea = id; }

  /**
   * Monta a sessão respeitando disciplina e dificuldade e garantindo exatamente
   * `qty` questões sempre que houver questões únicas suficientes:
   *
   * <ol>
   *   <li>Exercícios reais do backend (persistidos via /submit).</li>
   *   <li>Banco simulado da área (fallback local, XP registrado ao final).</li>
   *   <li>Questões do ENEM da disciplina equivalente (complemento quando faltar).</li>
   * </ol>
   *
   * A dificuldade é tratada como prioridade: questões da dificuldade escolhida vêm
   * primeiro e, se faltarem, completamos com as demais da mesma disciplina — sem
   * duplicar enunciados.
   */
  async startSession() {
    if (!this.selectedArea || this.loading) return;
    this.loading = true;
    this.loadingMessage = 'Buscando questões...';
    try {
      const area = this.areas.find(a => a.id === this.selectedArea);
      const category = this.areaToCategory[this.selectedArea];
      const enemDiscipline = this.areaToEnemDiscipline[this.selectedArea] ?? this.areaToEnem[this.selectedArea];

      let pool: QuizQuestion[] = [];
      let enemError = false;

      // 1) Exercícios reais do banco (XP persistido individualmente).
      if (category) {
        try {
          // Pede mais que `qty` para sobrar margem após priorizar a dificuldade.
          const exercises = await firstValueFrom(
            this.exerciseService.getExercisesByCategory(category, Math.max(this.qty * 4, 40))
          );
          pool = exercises
            .filter(e => e.type === 'multiple-choice' && e.options && e.options.length > 0)
            .map(e => this.toQuizQuestion(e));
        } catch {
          this.toastr.warning('Não foi possível carregar exercícios do servidor. Usando banco de demonstração.', 'Atenção');
        }
      }

      // 2) Banco simulado da área, se o banco real não cobrir `qty`.
      if (pool.length < this.qty) {
        const mockForArea = this.allQuestions
          .filter(q => !area || q.area === area.name)
          .map(q => this.toQuizQuestionFromMock(q));
        pool = this.mergeUnique(pool, mockForArea);
      }

      // 3) Complemento do ENEM (disciplina equivalente), se ainda faltar.
      if (pool.length < this.qty && enemDiscipline) {
        this.loadingMessage = 'Carregando questões do ENEM (pode levar alguns segundos)...';
        try {
          const deficit = Math.max(this.qty - pool.length, 1);
          const exercises = await firstValueFrom(
            this.exerciseService.getEnemQuestions(enemDiscipline, deficit)
          );
          const enemPool = exercises
            .filter(e => e.type === 'multiple-choice' && e.options && e.options.length > 0)
            .map(e => this.toEnemQuizQuestion(e));
          pool = this.mergeUnique(pool, enemPool);
        } catch {
          enemError = true;
        }
      }

      const selection = this.pickQuestions(pool, this.qty, this.difficulty, !!category);

      if (selection.length === 0) {
        if (!category && enemError) {
          this.toastr.error('Não foi possível carregar as questões do ENEM. Tente novamente.', 'Erro');
        } else {
          this.toastr.error('Nenhuma questão disponível para esta disciplina.', 'Erro');
        }
        return;
      }
      if (selection.length < this.qty) {
        this.toastr.warning(
          `Apenas ${selection.length} questão(ões) disponíveis — a sessão foi reduzida.`,
          'Atenção'
        );
      }

      this.sessionQuestions = selection;
      this.totalQuestions = selection.length;
      this.currentIndex = 0;
      this.earnedXp = 0;
      this.mockXpAccumulated = 0;
      this.mockCorrectAnswers = 0;
      this.correctAnswers = 0;
      this.combo = 0;
      this.maxCombo = 0;
      this.answered = false;
      this.selectedOption = null;
      this.lastXpEarned = 0;
      this.feedbackMessage = '';
      this.sessionActive = true;
      this.sessionFinished = false;

      // Inicia a contagem de tempo da sessão e da primeira questão.
      this.sessionStartedAt = Date.now();
      this.questionStartedAt = Date.now();

      // Garante que a primeira questão fique visível após substituir a tela de setup.
      window.scrollTo({ top: 0, behavior: 'auto' });
    } finally {
      this.loading = false;
      this.loadingMessage = '';
    }
  }

  /**
   * Concatena dois conjuntos de questões removendo duplicatas (por id real ou,
   * para questões simuladas/ENEM, pelo enunciado normalizado).
   */
  private mergeUnique(base: QuizQuestion[], extra: QuizQuestion[]): QuizQuestion[] {
    const seen = new Set<string>();
    const result: QuizQuestion[] = [];
    for (const q of [...base, ...extra]) {
      const key = q.exerciseId != null
        ? `ex:${q.exerciseId}`
        : `q:${q.text.trim().toLowerCase()}`;
      if (seen.has(key)) continue;
      seen.add(key);
      result.push(q);
    }
    return result;
  }

  /**
   * Seleciona até `qty` questões, priorizando a dificuldade escolhida e completando
   * com as demais da mesma disciplina. `respectDifficulty` é falso para áreas ENEM,
   * cuja API não expõe dificuldade (todas vêm como "medium").
   */
  private pickQuestions(pool: QuizQuestion[], qty: number, difficulty: string, respectDifficulty: boolean): QuizQuestion[] {
    if (!respectDifficulty) {
      return this.shuffle(pool).slice(0, qty);
    }
    const matching = pool.filter(q => q.difficulty === difficulty);
    if (matching.length >= qty) {
      return this.shuffle(matching).slice(0, qty);
    }
    const others = pool.filter(q => q.difficulty !== difficulty);
    return [...this.shuffle(matching), ...this.shuffle(others)].slice(0, qty);
  }

  selectOption(idx: number) {
    if (!this.answered) this.selectedOption = idx;
  }

  async confirmAnswer() {
    if (this.selectedOption === null || !this.currentQuestion || this.submitting) return;
    this.submitting = true;
    this.answered = true;

    const q = this.currentQuestion;
    const chosen = q.options[this.selectedOption];
    let correct: boolean;
    let xpEarned = 0;

    if (q.exerciseId != null) {
      // Exercício real: valida no servidor e usa a resposta como fonte de verdade.
      try {
        const timeSpent = Math.max(0, Math.round((Date.now() - this.questionStartedAt) / 1000));
        const res = await firstValueFrom(
          this.exerciseService.submitExercise({ exerciseId: q.exerciseId, answer: chosen.id, timeSpent })
        );
        correct = res.correct;
        xpEarned = res.xpEarned ?? 0;
        q.options.forEach(o => o.correct = o.id === res.correctAnswer);
        if (res.explanation) q.explanation = res.explanation;
      } catch {
        // Falha ao registrar: sem a resposta correta no cliente (removida do DTO
        // para não vazar o gabarito), não dá mais para validar offline. Não pontua.
        correct = false;
        this.toastr.warning('Não foi possível registrar a resposta no servidor. Tente novamente.', 'Atenção');
      }
    } else {
      // Questão simulada: validação local.
      correct = chosen.correct;
    }

    this.lastCorrect = correct;
    this.lastXpEarned = xpEarned;

    if (correct) {
      this.correctAnswers++;
      if (q.exerciseId == null) this.mockCorrectAnswers++;
      this.combo++;
      if (this.combo > this.maxCombo) this.maxCombo = this.combo;
      if (q.exerciseId == null) {
        // Bônus de combo aplicado somente às questões simuladas (XP controlado pelo servidor nas reais).
        xpEarned = this.mockXpFor(q, this.combo);
        this.lastXpEarned = xpEarned;
        this.mockXpAccumulated += xpEarned;
      }
      this.earnedXp += xpEarned;
      this.feedbackMessage = `Acertou! +${xpEarned} XP`;
    } else {
      this.combo = 0;
      if (q.exerciseId == null) {
        xpEarned = 1;
        this.lastXpEarned = xpEarned;
        this.earnedXp += 1;
        this.mockXpAccumulated += 1;
        this.feedbackMessage = 'Errado! +1 XP pela tentativa';
      } else {
        this.feedbackMessage = 'Errado!';
      }
    }

    this.submitting = false;
  }

  nextQuestion() {
    if (this.currentIndex + 1 >= this.totalQuestions) {
      this.sessionFinished = true;
      this.persistMockXp();
    } else {
      this.currentIndex++;
      this.selectedOption = null;
      this.answered = false;
      this.feedbackMessage = '';
      this.questionStartedAt = Date.now();
    }
  }

  /**
   * Persiste no backend o XP das questões simuladas/ENEM, que não passaram por
   * /submit individual. O XP de exercícios reais já foi persistido resposta a resposta.
   */
  private persistMockXp(): void {
    if (this.mockXpAccumulated <= 0) return;
    const xp = this.mockXpAccumulated;
    const area = this.areas.find(a => a.id === this.selectedArea);
    // Quando a prática veio de uma trilha, usa a categoria da trilha para que a
    // sessão seja creditada no progresso dela (ex.: "Física" em vez de "ENEM · Natureza").
    const category = this.trailCategory ?? (area ? area.name : 'Prática');
    const durationSeconds = Math.max(0, Math.round((Date.now() - this.sessionStartedAt) / 1000));
    // As questões reais já foram persistidas individualmente via /submit. A sessão
    // reportada aqui representa apenas as simuladas/ENEM, para que o total de
    // exercícios (conquistas/missões/estatísticas) não conte as reais duas vezes.
    const mockTotal = this.sessionQuestions.filter(q => q.exerciseId == null).length;
    this.mockXpAccumulated = 0;
    this.exerciseService.completeSession(xp, this.mockCorrectAnswers, mockTotal, category, durationSeconds).subscribe({
      error: () => this.toastr.warning('Não foi possível registrar o XP da sessão no servidor.', 'Atenção')
    });
  }

  restart() {
    this.sessionActive = false;
    this.sessionFinished = false;
    this.selectedArea = '';
    this.totalQuestions = 0;
    this.sessionQuestions = [];
    this.feedbackMessage = '';
    // A categoria da trilha de origem vale apenas para a sessão disparada por
    // "Começar a praticar"; uma nova sessão livre usa a área escolhida agora.
    this.trailCategory = null;
  }

  filterAreas() {
    const term = this.searchTerm.toLowerCase();
    this.filteredAreas = this.areas.filter(area =>
      area.name.toLowerCase().includes(term)
    );
    this.carouselIndex = 0;
  }

  scrollCarousel(direction: 'left' | 'right') {
    if (direction === 'left' && this.carouselIndex > 0) {
      this.carouselIndex--;
    } else if (direction === 'right' && this.carouselIndex < this.filteredAreas.length - this.carouselItemsVisible) {
      this.carouselIndex++;
    }
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
    localStorage.setItem('sidebarOpen', this.sidebarOpen.toString());
  }

  private toQuizQuestion(e: Exercise): QuizQuestion {
    return {
      exerciseId: e.id,
      area: e.category,
      areaIcon: this.categoryIcons[e.category] ?? '📚',
      difficulty: e.difficulty <= 1 ? 'easy' : e.difficulty <= 2 ? 'medium' : 'hard',
      text: e.question,
      options: (e.options || []).map(o => ({ id: o.id, text: o.text, correct: false })),
      explanation: e.explanation,
      xp: e.xpReward ?? 10,
      correctAnswer: e.correctAnswer ?? null
    };
  }

  private toEnemQuizQuestion(e: Exercise): QuizQuestion {
    const correct = e.correctAnswer ?? null;
    return {
      exerciseId: null,
      area: e.category,
      areaIcon: '📚',
      difficulty: 'medium',
      text: e.question,
      options: (e.options || []).map(o => ({ id: o.id, text: o.text, correct: o.id === correct })),
      explanation: e.explanation,
      xp: e.xpReward ?? 10,
      correctAnswer: correct
    };
  }

  private toQuizQuestionFromMock(q: MockQuestion): QuizQuestion {
    return {
      exerciseId: null,
      area: q.area,
      areaIcon: q.areaIcon,
      difficulty: q.difficulty,
      text: q.text,
      options: q.options.map((o, i) => ({
        id: String.fromCharCode(65 + i),
        text: o.text,
        correct: o.correct
      })),
      explanation: q.explanation,
      xp: q.xp,
      correctAnswer: null
    };
  }

  private mockXpFor(q: QuizQuestion, combo: number): number {
    let xp = q.xp;
    if (combo === 3) xp += 10;
    if (combo >= 5) xp += 20;
    return xp;
  }

  private shuffle<T>(items: T[]): T[] {
    const copy = [...items];
    for (let i = copy.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [copy[i], copy[j]] = [copy[j], copy[i]];
    }
    return copy;
  }
}
