import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

/**
 * Renderiza o Markdown simples devolvido pela IA do chat.
 *
 * Segurança (XSS): todo o conteúdo é escapado ANTES de qualquer formatação. Os
 * únicos elementos HTML injetados são os gerados aqui, e links só são emitidos
 * para protocolos http(s).
 *
 * Suporta: cabeçalhos (#/##/###), negrito (**x**), código inline (`x`), blocos de
 * código (```x```), listas não ordenadas (- / * / •) e ordenadas (1.), citações
 * (>) e links [texto](url).
 */
@Pipe({
  name: 'formatMessage',
  standalone: true
})
export class FormatMessagePipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) {}

  transform(text: string): SafeHtml {
    if (text == null) return '';
    let html = this.escapeHtml(String(text));
    html = this.renderMarkdown(html);
    // Só agora (texto já sanitizado) liberamos o HTML para o [innerHTML].
    return this.sanitizer.bypassSecurityTrustHtml(html);
  }

  private escapeHtml(value: string): string {
    return value
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  private renderMarkdown(escaped: string): string {
    // 1) Protege blocos de código (``` ... ```) em placeholders, para que o
    //    conteúdo não seja reinterpretado pelas regras de formatação abaixo.
    const blocks: string[] = [];
    let html = escaped.replace(/```([\s\S]*?)```/g, (_m, code: string) => {
      blocks.push(`<pre><code>${code.trim()}</code></pre>`);
      return `\u0000${blocks.length - 1}\u0000`;
    });

    // 2) Protege código inline (`...`) da mesma forma.
    const inlines: string[] = [];
    html = html.replace(/`([^`\n]+)`/g, (_m, code: string) => {
      inlines.push(`<code>${code}</code>`);
      return `\u0001${inlines.length - 1}\u0001`;
    });

    // 3) Cabeçalhos viram títulos em negrito (sem quebrar o layout do balão).
    html = html.replace(/^#{1,3}\s+(.+)$/gm, (_m, t) => `<strong class="md-heading">${t}</strong>`);

    // 4) Citações (linhas iniciando com ">").
    html = html.replace(/^&gt;\s?(.+)$/gm, (_m, t) => `<blockquote>${t}</blockquote>`);

    // 5) Listas não ordenadas: agrupa linhas consecutivas iniciadas por "-", "*" ou "•".
    html = html.replace(/(?:^|\n)(?:(?:[-*•])\s+[^\n]+(?:\n|$))+/g, (block) => {
      const items = block
        .split('\n')
        .filter(l => /^(?:[-*•])\s+/.test(l))
        .map(l => `<li>${l.replace(/^(?:[-*•])\s+/, '')}</li>`)
        .join('');
      return `\n<ul>${items}</ul>\n`;
    });

    // 6) Listas ordenadas: linhas iniciadas por "1.", "2.", ...
    html = html.replace(/(?:^|\n)(?:\d+\.\s+[^\n]+(?:\n|$))+/g, (block) => {
      const items = block
        .split('\n')
        .filter(l => /^\d+\.\s+/.test(l))
        .map(l => `<li>${l.replace(/^\d+\.\s+/, '')}</li>`)
        .join('');
      return `\n<ol>${items}</ol>\n`;
    });

    // 7) Negrito **texto**.
    html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');

    // 8) Links [texto](url) — somente http(s), texto já escapado.
    html = html.replace(
      /\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g,
      '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>'
    );

    // 9) "•" remanescente (fora de listas) vira bullet.
    html = html.replace(/•/g, '&bull;');

    // 10) Quebras de linha restantes.
    html = html.replace(/\n/g, '<br>');

    // 11) Restaura código inline e blocos.
    html = html.replace(/\u0001(\d+)\u0001/g, (_m, i) => inlines[Number(i)] ?? '');
    html = html.replace(/\u0000(\d+)\u0000/g, (_m, i) => blocks[Number(i)] ?? '');

    return html;
  }
}
