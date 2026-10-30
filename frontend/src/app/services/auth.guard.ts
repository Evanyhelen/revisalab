import { Injectable } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

/**
 * Impede o acesso às rotas protegidas do app sem um token válido.
 *
 * As telas internas (dashboard, trilhas, prática, chat, etc.) dependem de dados
 * do usuário logado; sem autenticação elas renderizariam apenas fallbacks mocados.
 * Aqui redirecionamos para o login, preservando a rota de origem para voltar após
 * a autenticação. As páginas públicas (home, sobre, fundamentação, impacto e o
 * próprio /auth) continuam acessíveis sem login.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  const returnUrl = router.routerState.snapshot.url;
  return router.createUrlTree(['/auth/login'], {
    queryParams: returnUrl && returnUrl !== '/' ? { returnUrl } : undefined
  });
};
