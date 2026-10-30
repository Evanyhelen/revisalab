import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { firstValueFrom } from 'rxjs';
import { TrailService, Trail as TrailDTO, TrailDetail, StudyResources, StudyArticle, StudyVideo, StudyExtras } from '../services/trail.service';

export interface Trail {
  id: string;
  icon: string;
  name: string;
  area: string;
  areaId: string;
  description: string;
  totalXp: number;
  questions: number;
  hours: number;
  modules: number;
  progress: number;
  gradient: string;
}

/** Metadados de cada categoria para o filtro e apresentação. */
const CATEGORY_META: Record<string, { id: string; icon: string; emoji: string; gradient: string }> = {
  'Matemática': { id: 'matematica', icon: 'calculate', emoji: '📐', gradient: 'linear-gradient(135deg, #1a2980, #26d0ce)' },
  'Português': { id: 'portugues', icon: 'menu_book', emoji: '📖', gradient: 'linear-gradient(135deg, #26d0ce, #1a9b98)' },
  'Física': { id: 'fisica', icon: 'science', emoji: '🔭', gradient: 'linear-gradient(135deg, #11998e, #38ef7d)' },
  'Química': { id: 'quimica', icon: 'biotech', emoji: '⚗️', gradient: 'linear-gradient(135deg, #8b5cf6, #6d28d9)' },
  'Raciocínio Lógico': { id: 'logica', icon: 'psychology', emoji: '🧠', gradient: 'linear-gradient(135deg, #f5a623, #f7c948)' },
  'História': { id: 'historia', icon: 'history_edu', emoji: '🏛️', gradient: 'linear-gradient(135deg, #c0392b, #e74c3c)' },
  'Geografia': { id: 'geografia', icon: 'public', emoji: '🌎', gradient: 'linear-gradient(135deg, #2980b9, #3498db)' },
  'Biologia': { id: 'biologia', icon: 'eco', emoji: '🧬', gradient: 'linear-gradient(135deg, #16a085, #1abc9c)' },
  'Redação': { id: 'redacao', icon: 'edit_note', emoji: '✍️', gradient: 'linear-gradient(135deg, #e11d48, #fb7185)' },
  'Filosofia': { id: 'filosofia', icon: 'psychology', emoji: '🤔', gradient: 'linear-gradient(135deg, #7c3aed, #a78bfa)' },
  'Sociologia': { id: 'sociologia', icon: 'groups', emoji: '👥', gradient: 'linear-gradient(135deg, #0ea5e9, #38bdf8)' },
  'Inglês': { id: 'ingles', icon: 'translate', emoji: '🗣️', gradient: 'linear-gradient(135deg, #2563eb, #60a5fa)' }
};

/** Categoria da trilha → disciplina ENEM equivalente (conteúdo real da API do ENEM). */
const AREA_TO_ENEM: Record<string, string> = {
  matematica: 'enem-matematica',
  portugues: 'enem-linguagens',
  fisica: 'enem-natureza',
  quimica: 'enem-natureza',
  biologia: 'enem-natureza',
  historia: 'enem-humanas',
  geografia: 'enem-humanas',
  logica: 'enem-matematica',
  redacao: 'enem-linguagens',
  filosofia: 'enem-humanas',
  sociologia: 'enem-humanas',
  ingles: 'enem-linguagens'
};

@Component({
  selector: 'app-aulas',
  standalone: false,
  templateUrl: './aulas.html',
  styleUrl: './aulas.scss'
})
export class Aulas implements OnInit {
  sidebarOpen = true;
  searchText = '';
  activeArea = 'all';
  filterCarouselIndex = 0;
  filterItemsVisible = 6;

  // Estado do catálogo de trilhas.
  loading = false;
  loadError = false;

  // Modal de material de estudo (Wikipedia + YouTube).
  activeTrail: Trail | null = null;
  trailResources: StudyResources | null = null;
  resourcesLoading = false;
  resourcesError = false;

  // Detalhe da trilha (módulos + etapa atual) para "Iniciar Trilha"/"Continuar".
  trailDetail: TrailDetail | null = null;

  // Etapa selecionada pelo usuário (navegação entre os módulos da trilha).
  selectedModule: { title: string; orderNumber: number } | null = null;

  // Sequência das requisições de material de estudo, para descartar respostas
  // atrasadas quando o usuário troca de etapa rapidamente.
  private resourcesSeq = 0;

  constructor(
    private trailService: TrailService,
    private router: Router,
    private sanitizer: DomSanitizer
  ) {
    const savedState = localStorage.getItem('sidebarOpen');
    if (savedState !== null) {
      this.sidebarOpen = savedState === 'true';
    }
  }

  // Áreas do filtro (reconstruídas a partir das trilhas reais; fallback mocado abaixo).
  areas = [
    { id: 'matematica', icon: 'calculate', name: 'Matemática' },
    { id: 'portugues', icon: 'menu_book', name: 'Português' },
    { id: 'fisica', icon: 'science', name: 'Física' },
    { id: 'quimica', icon: 'biotech', name: 'Química' },
    { id: 'biologia', icon: 'eco', name: 'Biologia' },
    { id: 'historia', icon: 'history_edu', name: 'História' },
    { id: 'geografia', icon: 'public', name: 'Geografia' },
    { id: 'logica', icon: 'psychology', name: 'Raciocínio Lógico' }
  ];

  get maxFilterIndex(): number {
    return Math.max(0, this.areas.length + 1 - this.filterItemsVisible);
  }

  // Fallback mocado (backend indisponível).
  trails: Trail[] = [
    {
      id: '1', icon: '📐', name: 'Matemática Básica',
      area: 'Matemática', areaId: 'matematica',
      description: 'Fundamentos de aritmética, álgebra, equações e funções para nivelar a base.',
      totalXp: 800, questions: 150, hours: 12, modules: 6, progress: 62,
      gradient: 'linear-gradient(135deg, #1a2980, #26d0ce)'
    },
    {
      id: '2', icon: '📊', name: 'Geometria e Trigonometria',
      area: 'Matemática', areaId: 'matematica',
      description: 'Geometria plana, espacial, trigonometria e análise combinatória.',
      totalXp: 650, questions: 120, hours: 10, modules: 5, progress: 0,
      gradient: 'linear-gradient(135deg, #2c3e8c, #1a2980)'
    },
    {
      id: '3', icon: '📖', name: 'Interpretação de Texto',
      area: 'Português', areaId: 'portugues',
      description: 'Gêneros textuais, coerência, coesão e interpretação crítica de textos.',
      totalXp: 500, questions: 80, hours: 8, modules: 4, progress: 35,
      gradient: 'linear-gradient(135deg, #26d0ce, #1a9b98)'
    },
    {
      id: '4', icon: '✍️', name: 'Gramática Essencial',
      area: 'Português', areaId: 'portugues',
      description: 'Morfologia, sintaxe, concordância e regência para escrita correta.',
      totalXp: 420, questions: 70, hours: 7, modules: 4, progress: 0,
      gradient: 'linear-gradient(135deg, #00b09b, #26d0ce)'
    },
    {
      id: '5', icon: '🔭', name: 'Física Mecânica',
      area: 'Física', areaId: 'fisica',
      description: 'Cinemática, dinâmica, energia, trabalho e impulso.',
      totalXp: 600, questions: 90, hours: 9, modules: 5, progress: 0,
      gradient: 'linear-gradient(135deg, #27ae60, #2ecc71)'
    },
    {
      id: '6', icon: '⚡', name: 'Eletricidade e Óptica',
      area: 'Física', areaId: 'fisica',
      description: 'Circuitos elétricos, campo elétrico, óptica geométrica e ondulatória.',
      totalXp: 520, questions: 80, hours: 8, modules: 4, progress: 0,
      gradient: 'linear-gradient(135deg, #11998e, #38ef7d)'
    },
    {
      id: '7', icon: '⚗️', name: 'Química Geral',
      area: 'Química', areaId: 'quimica',
      description: 'Tabela periódica, ligações químicas, mol e estequiometria.',
      totalXp: 480, questions: 75, hours: 7, modules: 4, progress: 0,
      gradient: 'linear-gradient(135deg, #8b5cf6, #6d28d9)'
    },
    {
      id: '8', icon: '🔬', name: 'Química Orgânica',
      area: 'Química', areaId: 'quimica',
      description: 'Funções orgânicas, isomeria, reações e polímeros.',
      totalXp: 440, questions: 70, hours: 6, modules: 4, progress: 0,
      gradient: 'linear-gradient(135deg, #7c3aed, #8b5cf6)'
    },
    {
      id: '9', icon: '🧠', name: 'Lógica Proposicional',
      area: 'Raciocínio Lógico', areaId: 'logica',
      description: 'Proposições, conectivos, tabelas verdade e inferências lógicas.',
      totalXp: 380, questions: 60, hours: 6, modules: 3, progress: 18,
      gradient: 'linear-gradient(135deg, #f5a623, #f7c948)'
    },
    {
      id: '10', icon: '🔢', name: 'Raciocínio Numérico',
      area: 'Raciocínio Lógico', areaId: 'logica',
      description: 'Sequências, progressões, probabilidade e estatística básica.',
      totalXp: 360, questions: 55, hours: 5, modules: 3, progress: 0,
      gradient: 'linear-gradient(135deg, #e67e22, #f5a623)'
    },
    {
      id: '11', icon: '🧬', name: 'Biologia Celular',
      area: 'Biologia', areaId: 'biologia',
      description: 'Citologia, organelas, metabolismo celular e divisão celular.',
      totalXp: 520, questions: 85, hours: 8, modules: 4, progress: 0,
      gradient: 'linear-gradient(135deg, #16a085, #1abc9c)'
    },
    {
      id: '12', icon: '🌱', name: 'Genética e Evolução',
      area: 'Biologia', areaId: 'biologia',
      description: 'Leis de Mendel, genética molecular, evolução e seleção natural.',
      totalXp: 480, questions: 75, hours: 7, modules: 4, progress: 0,
      gradient: 'linear-gradient(135deg, #27ae60, #16a085)'
    },
    {
      id: '13', icon: '📜', name: 'História do Brasil',
      area: 'História', areaId: 'historia',
      description: 'Colônia, Império, República e Brasil contemporâneo.',
      totalXp: 450, questions: 70, hours: 7, modules: 4, progress: 0,
      gradient: 'linear-gradient(135deg, #c0392b, #e74c3c)'
    },
    {
      id: '14', icon: '🏛️', name: 'História Geral',
      area: 'História', areaId: 'historia',
      description: 'Antiguidade, Idade Média, Moderna e Contemporânea.',
      totalXp: 480, questions: 75, hours: 8, modules: 4, progress: 0,
      gradient: 'linear-gradient(135deg, #e74c3c, #ec7063)'
    },
    {
      id: '15', icon: '🌍', name: 'Geografia Física',
      area: 'Geografia', areaId: 'geografia',
      description: 'Relevo, clima, hidrografia e biomas do Brasil e do mundo.',
      totalXp: 420, questions: 65, hours: 6, modules: 4, progress: 0,
      gradient: 'linear-gradient(135deg, #2980b9, #3498db)'
    },
    {
      id: '16', icon: '🏙️', name: 'Geografia Humana',
      area: 'Geografia', areaId: 'geografia',
      description: 'População, urbanização, economia e geopolítica.',
      totalXp: 440, questions: 68, hours: 7, modules: 4, progress: 0,
      gradient: 'linear-gradient(135deg, #3498db, #5dade2)'
    }
  ];

  ngOnInit(): void {
    this.loadTrails();
  }

  private async loadTrails(): Promise<void> {
    this.loading = true;
    this.loadError = false;
    try {
      const trails = await firstValueFrom(this.trailService.getAllTrails());
      this.trails = trails.map(t => this.toTrail(t));

      // Reconstrói o filtro de áreas a partir das categorias realmente disponíveis.
      const seen = new Set<string>();
      const areas: { id: string; icon: string; name: string }[] = [];
      for (const t of trails) {
        if (seen.has(t.category)) continue;
        seen.add(t.category);
        const meta = CATEGORY_META[t.category];
        areas.push({
          id: meta?.id ?? this.slug(t.category),
          icon: meta?.icon ?? 'menu_book',
          name: t.category
        });
      }
      if (areas.length > 0) this.areas = areas;
    } catch {
      // Degrada para o catálogo de demonstração, sinalizando a indisponibilidade.
      this.loadError = true;
    } finally {
      this.loading = false;
    }
  }

  private toTrail(t: TrailDTO): Trail {
    const meta = CATEGORY_META[t.category];
    const progress = Math.round((t.progress ?? 0) * 100);
    return {
      id: String(t.id),
      icon: meta?.emoji ?? '📚',
      name: t.title,
      area: t.category,
      areaId: meta?.id ?? this.slug(t.category),
      description: t.description,
      totalXp: (t.totalExercises ?? 0) * 10,
      questions: t.totalExercises ?? 0,
      hours: t.estimatedHours ?? 0,
      modules: t.totalModules ?? 0,
      progress,
      gradient: meta?.gradient ?? 'linear-gradient(135deg, #64748b, #94a3b8)'
    };
  }

  private slug(name: string): string {
    return name.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/[^a-z0-9]+/g, '-');
  }

  get filteredTrails(): Trail[] {
    return this.trails.filter(t => {
      const matchesArea = this.activeArea === 'all' || t.areaId === this.activeArea;
      const matchesSearch = t.name.toLowerCase().includes(this.searchText.toLowerCase()) ||
        t.description.toLowerCase().includes(this.searchText.toLowerCase());
      return matchesArea && matchesSearch;
    });
  }

  setArea(area: string): void {
    this.activeArea = area;
  }

  get hasActiveFilters(): boolean {
    return this.activeArea !== 'all' || this.searchText.trim().length > 0;
  }

  clearFilters(): void {
    this.activeArea = 'all';
    this.searchText = '';
  }

  /** Conteúdo externo disponível no modal (vide/anotação), com narrowing para o template. */
  get video(): StudyVideo | null {
    return this.trailResources?.video ?? null;
  }

  get article(): StudyArticle | null {
    return this.trailResources?.article ?? null;
  }

  get extras(): StudyExtras | null {
    return this.trailResources?.extras ?? null;
  }

  /** Módulos da trilha aberta (etapas estruturadas do percurso). */
  get trailModules() {
    return this.trailDetail?.modules ?? [];
  }

  /** Título da etapa atual (primeira não concluída) para o rótulo do botão. */
  get currentStepTitle(): string {
    return this.selectedModule?.title ?? 'esta trilha';
  }

  get isResumingTrail(): boolean {
    return (this.activeTrail?.progress ?? 0) > 0;
  }

  /** Rótulo do botão principal do modal, sensível ao progresso salvo. */
  get practiceButtonLabel(): string {
    const prefix = this.isResumingTrail ? 'Continuar em' : 'Praticar';
    return `${prefix} ${this.currentStepTitle}`;
  }

  isCurrentStep(orderNumber: number): boolean {
    return orderNumber === this.selectedModule?.orderNumber;
  }

  /**
   * Abre o modal de material de estudo da trilha, buscando conteúdo real da
   * Wikipedia (teoria) e do YouTube (vídeo/busca). Falha degrada graciosamente:
   * o botão de praticar continua disponível.
   */
  openTrail(trail: Trail): void {
    this.activeTrail = trail;
    this.trailResources = null;
    this.trailDetail = null;
    this.selectedModule = null;
    this.resourcesLoading = true;
    this.resourcesError = false;

    // Carrega as etapas e, em seguida, a teoria da etapa atual (primeira não concluída).
    this.trailService.getTrailById(Number(trail.id)).subscribe({
      next: (detail) => {
        if (this.activeTrail?.id !== trail.id) return;
        this.trailDetail = detail;
        this.selectedModule = this.defaultModule();
        this.loadResources(this.selectedModule.title);
      },
      error: () => {
        if (this.activeTrail?.id !== trail.id) return;
        // Sem detalhe de módulos, degrada para a teoria da trilha inteira.
        this.loadResources(trail.name);
      }
    });
  }

  /** Seleciona uma etapa e carrega o material de estudo específico dela. */
  selectModule(module: { title: string; orderNumber: number }): void {
    if (!this.activeTrail) return;
    this.selectedModule = { title: module.title, orderNumber: module.orderNumber };
    this.loadResources(module.title);
  }

  /** Etapa a ser destacada por padrão: a primeira ainda não concluída. */
  private defaultModule(): { title: string; orderNumber: number } {
    const modules = this.trailDetail?.modules ?? [];
    const current = modules.find(m => !m.completed) ?? modules[0];
    if (!current) {
      return { title: this.activeTrail?.name ?? 'esta trilha', orderNumber: 0 };
    }
    return { title: current.title, orderNumber: current.orderNumber };
  }

  /** Busca o material de estudo (vídeo + artigo) para o título informado. */
  private loadResources(title: string): void {
    if (!this.activeTrail) return;
    const trail = this.activeTrail;
    const seq = ++this.resourcesSeq;
    this.resourcesLoading = true;
    this.resourcesError = false;
    this.trailService.getStudyResources(trail.area, title).subscribe({
      next: (resources) => {
        if (seq !== this.resourcesSeq || this.activeTrail?.id !== trail.id) return;
        this.trailResources = resources;
        this.resourcesLoading = false;
      },
      error: () => {
        if (seq !== this.resourcesSeq || this.activeTrail?.id !== trail.id) return;
        this.resourcesError = true;
        this.resourcesLoading = false;
      }
    });
  }

  closeTrail(): void {
    this.activeTrail = null;
    this.trailResources = null;
    this.trailDetail = null;
    this.resourcesError = false;
    this.resourcesLoading = false;
  }

  /**
   * Abre a sessão de prática correspondente à trilha, já com a disciplina do ENEM
   * selecionada (conteúdo real da API externa). Raciocínio Lógico não possui
   * disciplina própria no ENEM, então cai na Matemática.
   */
  startPractice(trail: Trail): void {
    this.closeTrail();
    const area = AREA_TO_ENEM[trail.areaId] ?? trail.areaId;
    // `cat` = categoria da trilha: a sessão de ENEM é creditada no progresso da
    // trilha correspondente, em vez de ficar apenas como prática livre.
    this.router.navigate(['/praticar'], { queryParams: { area, cat: trail.area } });
  }

  safeEmbedUrl(url: string | null | undefined): SafeResourceUrl | null {
    return url ? this.sanitizer.bypassSecurityTrustResourceUrl(url) : null;
  }

  scrollFilters(direction: 'left' | 'right'): void {
    if (direction === 'left' && this.filterCarouselIndex > 0) {
      this.filterCarouselIndex--;
    } else if (direction === 'right' && this.filterCarouselIndex < this.maxFilterIndex) {
      this.filterCarouselIndex++;
    }
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
    localStorage.setItem('sidebarOpen', this.sidebarOpen.toString());
  }
}
