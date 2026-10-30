import { Routes } from '@angular/router';
import { authGuard } from './services/auth.guard';

export const routes: Routes = [
  { path: '', loadChildren: () => import('./home/home-module').then(m => m.HomeModule) },
  { path: 'auth', loadChildren: () => import('./auth/auth-module').then(m => m.AuthModule) },
  // Páginas públicas (conteúdo institucional do TCC) — acessíveis sem login.
  { path: 'sobre', loadChildren: () => import('./sobre/sobre-module').then(m => m.SobreModule) },
  { path: 'fundamentacao', loadChildren: () => import('./fundamentacao/fundamentacao-module').then(m => m.FundamentacaoModule) },
  { path: 'impacto', loadChildren: () => import('./impacto/impacto-module').then(m => m.ImpactoModule) },
  // Telas protegidas — exigem usuário autenticado.
  { path: 'dashboard', loadChildren: () => import('./dashboard/dashboard-module').then(m => m.DashboardModule), canActivate: [authGuard] },
  { path: 'trilhas', loadChildren: () => import('./aulas/aulas-module').then(m => m.AulasModule), canActivate: [authGuard] },
  { path: 'praticar', loadChildren: () => import('./quiz/quiz-module').then(m => m.QuizModule), canActivate: [authGuard] },
  { path: 'desempenho', loadChildren: () => import('./relatorios/relatorios-module').then(m => m.RelatoriosModule), canActivate: [authGuard] },
  { path: 'conquistas', loadChildren: () => import('./biblioteca/biblioteca-module').then(m => m.BibliotecaModule), canActivate: [authGuard] },
  { path: 'ranking', loadChildren: () => import('./ranking/ranking-module').then(m => m.RankingModule), canActivate: [authGuard] },
  { path: 'missoes', loadChildren: () => import('./missoes/missoes-module').then(m => m.MissoesModule), canActivate: [authGuard] },
  { path: 'construtor-base', loadComponent: () => import('./construtor-base/construtor-base').then(m => m.ConstrutorBaseComponent), canActivate: [authGuard] },
  { path: 'chat', loadChildren: () => import('./chat/chat-module').then(m => m.ChatModule), canActivate: [authGuard] },
  { path: 'feedback', loadChildren: () => import('./feedback/feedback-module').then(m => m.FeedbackModule), canActivate: [authGuard] },
  { path: 'perfil', loadChildren: () => import('./perfil/perfil-module').then(m => m.PerfilModule), canActivate: [authGuard] },
  { path: '**', redirectTo: '/' }
];
