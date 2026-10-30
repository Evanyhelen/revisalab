package com.revisalab.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envio real de e-mail via SMTP.
 *
 * Ativado quando {@code app.mail.mode=smtp}. Requer as propriedades padrão do
 * Spring Mail (todas sobrescrevíveis por variável de ambiente):
 * <pre>
 *   SPRING_MAIL_HOST=smtp.example.com
 *   SPRING_MAIL_PORT=587
 *   SPRING_MAIL_USERNAME=user
 *   SPRING_MAIL_PASSWORD=secret
 *   SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
 *   SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
 *   SPRING_MAIL_FROM=no-reply@revisalab.com
 * </pre>
 */
@Service
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "smtp")
public class SmtpMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:no-reply@revisalab.com}")
    private String from;

    public SmtpMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendPasswordResetEmail(String to, String userName, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Redefinição de senha - RevisaLab");
        message.setText(String.format(
                "Olá, %s!%n%nRecebemos uma solicitação para redefinir sua senha na RevisaLab.%n" +
                "Para criar uma nova senha, acesse o link abaixo (válido por tempo limitado):%n%n%s%n%n" +
                "Se você não solicitou essa alteração, ignore este e-mail.%n%nEquipe RevisaLab",
                userName, resetLink));

        mailSender.send(message);
        log.info("E-mail de recuperação enviado para {}", to);
    }
}
