import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FormatMessagePipe } from './format-message.pipe';
import { ChatService } from '../services/chat.service';
import { AuthService } from '../services/auth.service';

interface Message {
  id: string;
  text: string;
  sender: 'user' | 'ai';
  timestamp: Date;
}

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatChipsModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    FormatMessagePipe
  ],
  templateUrl: './chat.html',
  styleUrl: './chat.scss',
})
export class ChatComponent implements OnInit, AfterViewChecked {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;

  sidebarOpen = true;
  messages: Message[] = [];
  userInput: string = '';
  isTyping: boolean = false;
  shouldScroll: boolean = false;
  isLoading: boolean = false;
  hasError: boolean = false;
  // Incrementado a cada "Nova conversa" para descartar respostas de IA pendentes
  // que chegariam depois do histórico ter sido limpo.
  private requestSeq = 0;

  suggestedQuestions: string[] = [
    'Como fazer uma boa redação?',
    'O que é equação de 2º grau?',
    'Estrutura da célula',
    'Como usar estatística?'
  ];

  constructor(
    private chatService: ChatService,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    // Inicia com mensagens vazias para mostrar o header grande
    this.messages = [];

    // Recupera o estado da sidebar do localStorage
    const savedState = localStorage.getItem('sidebarOpen');
    if (savedState !== null) {
      this.sidebarOpen = savedState === 'true';
    }

    // Restaura o histórico da conversa persistido no backend.
    this.loadHistory();
  }

  /** Carrega as mensagens anteriores do backend, preservando a conversa. */
  private loadHistory(): void {
    this.chatService.getHistory().subscribe({
      next: (history) => {
        // Se o usuário já enviou mensagens enquanto o histórico estava em voo,
        // não sobrescreve a conversa atual com as mensagens antigas (corrida).
        if (this.messages.length > 0) return;
        if (history && history.length > 0) {
          this.messages = history.map((h, i) => ({
            id: `hist-${i}`,
            text: h.content,
            sender: h.role === 'user' ? 'user' : 'ai',
            timestamp: new Date(h.timestamp)
          }));
          this.shouldScroll = true;
        }
      },
      error: () => {
        // Sem histórico (ou falha): mantém a tela vazia, sem bloquear o chat.
      }
    });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  sendMessage(): void {
    if (!this.userInput.trim() || this.isLoading) return;

    // Se for a primeira mensagem, adiciona a mensagem de boas-vindas da IA primeiro
    if (this.messages.length === 0) {
      this.messages.push({
        id: '0',
        text: 'Olá! 👋 Sou a IA do RevisaLab. Como posso te ajudar hoje? Posso tirar dúvidas sobre redação, gramática, estrutura textual e muito mais!',
        sender: 'ai',
        timestamp: new Date()
      });
    }

    // Adiciona mensagem do usuário
    const userMessage: Message = {
      id: Date.now().toString(),
      text: this.userInput,
      sender: 'user',
      timestamp: new Date()
    };
    this.messages.push(userMessage);
    this.shouldScroll = true;

    const userQuestion = this.userInput;
    this.userInput = '';

    // Indica que está carregando
    this.isLoading = true;
    this.isTyping = true;
    this.hasError = false;

    // Identifica esta requisição para descartar a resposta caso o usuário
    // limpe a conversa ("Nova conversa") enquanto ela ainda está pendente.
    const seq = ++this.requestSeq;

    // Chama o backend real
    this.chatService.sendMessage(userQuestion).subscribe({
      next: (response) => {
        if (seq !== this.requestSeq) return; // conversa foi limpa enquanto aguardava
        // Adiciona resposta da IA
        this.messages.push({
          id: (Date.now() + 1).toString(),
          text: response.answer,
          sender: 'ai',
          timestamp: new Date()
        });
        this.shouldScroll = true;

        // Atualiza XP do usuário se houve ganho
        if (response.xpEarned > 0) {
          this.authService.updateUserStats(
            response.userXp,
            response.userLevel,
            this.authService.getCurrentUser()?.streak || 0
          );

          this.snackBar.open(`+${response.xpEarned} XP ganhos!`, 'OK', {
            duration: 3000,
            horizontalPosition: 'right',
            verticalPosition: 'top',
            panelClass: ['success-snackbar']
          });
        }

        this.isLoading = false;
        this.isTyping = false;
      },
      error: (error) => {
        if (seq !== this.requestSeq) return; // conversa foi limpa enquanto aguardava
        this.hasError = true;

        // Mensagem de erro amigável
        let errorMessage = '😔 Desculpe, não consegui processar sua pergunta no momento.';

        if (error.status === 503) {
          errorMessage = '🔧 O serviço de IA está temporariamente indisponível. Tente novamente em instantes.';
        } else if (error.status === 401) {
          errorMessage = '🔒 Sua sessão expirou. Por favor, faça login novamente.';
        } else if (error.status === 0) {
          errorMessage = '📡 Não foi possível conectar ao servidor. Verifique sua conexão.';
        }

        this.messages.push({
          id: (Date.now() + 1).toString(),
          text: errorMessage,
          sender: 'ai',
          timestamp: new Date()
        });
        this.shouldScroll = true;

        this.snackBar.open('Erro ao enviar mensagem', 'Fechar', {
          duration: 5000,
          horizontalPosition: 'right',
          verticalPosition: 'top',
          panelClass: ['error-snackbar']
        });

        this.isLoading = false;
        this.isTyping = false;
      }
    });
  }

  useSuggestedQuestion(question: string): void {
    this.userInput = question;
    this.sendMessage();
  }

  scrollToBottom(): void {
    try {
      this.messagesContainer.nativeElement.scrollTop = 
        this.messagesContainer.nativeElement.scrollHeight;
    } catch(err) { }
  }

  clearChat(): void {
    this.messages = [];
    // Invalida qualquer resposta de IA ainda pendente para que ela não
    // reapareça na conversa recém-limpa.
    this.requestSeq++;
    this.isLoading = false;
    this.isTyping = false;
    this.hasError = false;
    // Também apaga o histórico persistido para que a conversa não volte ao recarregar.
    this.chatService.clearHistory().subscribe({ error: () => {} });
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
    // Salva o estado no localStorage
    localStorage.setItem('sidebarOpen', this.sidebarOpen.toString());
  }
}
