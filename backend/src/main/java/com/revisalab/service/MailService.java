package com.revisalab.service;

/**
 * Abstração para o envio de e-mails transacionais da plataforma.
 *
 * Duas implementações são selecionadas por configuração ({@code app.mail.mode}):
 * <ul>
 *   <li>{@code log} (padrão) — mecanismo de captura para desenvolvimento, grava o
 *       e-mail (incluindo o link de recuperação) no log e em um arquivo local.</li>
 *   <li>{@code smtp} — envia de fato via SMTP, usando as variáveis
 *       {@code SPRING_MAIL_HOST}, {@code SPRING_MAIL_PORT}, {@code SPRING_MAIL_USERNAME},
 *       {@code SPRING_MAIL_PASSWORD} e {@code SPRING_MAIL_FROM}.</li>
 * </ul>
 */
public interface MailService {

    /**
     * Envia o e-mail com o link de recuperação de senha.
     *
     * @param to        endereço de destino
     * @param userName  nome do usuário (para personalização)
     * @param resetLink URL completa de redefinição de senha
     */
    void sendPasswordResetEmail(String to, String userName, String resetLink);
}
