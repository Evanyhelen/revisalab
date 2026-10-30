import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule
  ],
  template: `
    <footer class="footer">
      <div class="footer-content">
        <div class="footer-section">
          <div class="footer-logo">
            <span class="logo-icon">📖</span>
            <span class="logo-text">RevisaLab</span>
          </div>
          <p>Plataforma gamificada para nivelamento acadêmico e apoio à permanência estudantil no ensino superior.</p>
        </div>

        <div class="footer-section">
          <h4>Plataforma</h4>
          <ul>
            <li><a routerLink="/dashboard">Dashboard</a></li>
            <li><a routerLink="/trilhas">Trilhas</a></li>
            <li><a routerLink="/praticar">Praticar</a></li>
            <li><a routerLink="/desempenho">Desempenho</a></li>
            <li><a routerLink="/conquistas">Conquistas</a></li>
            <li><a routerLink="/ranking">Ranking</a></li>
          </ul>
        </div>

        <div class="footer-section">
          <h4>Recursos</h4>
          <ul>
            <li><a [routerLink]="['/']" fragment="como-funciona">Como Funciona</a></li>
            <li><a [routerLink]="['/']" fragment="funcionalidades">Funcionalidades</a></li>
            <li><a [routerLink]="['/']" fragment="depoimentos">Depoimentos</a></li>
            <li><a routerLink="/auth/register">Criar Conta</a></li>
          </ul>
        </div>

        <div class="footer-section">
          <h4>Projeto TCC</h4>
          <div class="tcc-info">
            <p><strong>Autora:</strong> Evanyhelen Martins Farias</p>
            <p><strong>Orientador:</strong> Allan Christian Krainski Ferrari</p>
            <p><strong>Curso:</strong> Engenharia de Software</p>
            <p><strong>Ano:</strong> 2026</p>
          </div>
        </div>
      </div>

      <div class="footer-bottom">
        <div class="footer-copyright">
          <p>RevisaLab – Plataforma Web Gamificada para Nivelamento Acadêmico – Projeto Final de Curso – 2026</p>
          <p>Curso de Engenharia de Software</p>
        </div>
        <button class="scroll-to-top" (click)="scrollToTop()" title="Voltar ao topo">
          <mat-icon>arrow_upward</mat-icon>
        </button>
      </div>
    </footer>
  `,
  styles: [`
    .footer {
      background: linear-gradient(135deg, #0f1a5c 0%, #1a2980 100%);
      color: white;
      font-family: 'Inter', sans-serif;
    }

    .footer-content {
      max-width: 1280px;
      margin: 0 auto;
      padding: 60px 24px 40px;
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 40px;
    }

    .footer-section {
      .footer-logo {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 16px;
        
        .logo-icon {
          font-size: 24px;
        }
        
        .logo-text {
          font-size: 24px;
          font-weight: 800;
          color: white;
          letter-spacing: -0.5px;
        }
        
        .logo-plus {
          color: #26d0ce;
        }
      }

      h4 {
        margin-bottom: 16px;
        color: #26d0ce;
        font-size: 16px;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      p {
        color: rgba(255,255,255,0.7);
        line-height: 1.6;
        margin-bottom: 12px;
        font-size: 14px;
      }

      ul {
        list-style: none;
        padding: 0;
        margin: 0;

        li {
          margin-bottom: 10px;

          a {
            color: rgba(255,255,255,0.7);
            text-decoration: none;
            font-size: 14px;
            transition: all 0.2s ease;
            display: inline-block;

            &:hover {
              color: #26d0ce;
              transform: translateX(4px);
            }
          }
        }
      }
    }

    .tcc-info {
      p {
        color: rgba(255,255,255,0.7);
        font-size: 13px;
        margin-bottom: 8px;
        
        strong {
          color: rgba(255,255,255,0.9);
          font-weight: 600;
        }
      }
    }

    .footer-bottom {
      background: rgba(0, 0, 0, 0.3);
      padding: 24px;
      border-top: 1px solid rgba(255,255,255,0.1);
      display: flex;
      align-items: center;
      justify-content: space-between;
      position: relative;

      .footer-copyright {
        flex: 1;
        text-align: center;

        p {
          margin: 4px 0;
          color: rgba(255,255,255,0.6);
          font-size: 13px;
          
          &:first-child {
            font-weight: 600;
            color: rgba(255,255,255,0.8);
          }
        }
      }

      .scroll-to-top {
        position: absolute;
        right: 24px;
        background: rgba(255, 255, 255, 0.1);
        border: 1px solid rgba(255, 255, 255, 0.2);
        border-radius: 50%;
        width: 48px;
        height: 48px;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: all 0.3s ease;
        color: white;

        mat-icon {
          font-size: 24px;
          width: 24px;
          height: 24px;
        }

        &:hover {
          background: rgba(255, 255, 255, 0.2);
          transform: translateY(-4px);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
        }

        &:active {
          transform: translateY(-2px);
        }
      }
    }

    @media (max-width: 768px) {
      .footer-content {
        grid-template-columns: 1fr;
        text-align: center;
        padding: 40px 20px 30px;
        gap: 32px;
      }

      .footer-section {
        .footer-logo {
          justify-content: center;
        }
        
        ul li a:hover {
          transform: none;
        }
      }

      .footer-bottom {
        flex-direction: column;
        gap: 16px;

        .scroll-to-top {
          position: static;
          margin-top: 16px;
        }
      }
    }
  `]
})
export class FooterComponent {
  scrollToTop(): void {
    window.scrollTo({
      top: 0,
      behavior: 'smooth'
    });
  }
}