package com.revisalab.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mecanismo de captura de e-mail para desenvolvimento/testes.
 *
 * Em vez de depender de um servidor SMTP, grava o conteúdo do e-mail no log da
 * aplicação e em um arquivo local ({@code app.password-reset.dev-capture-file}).
 * Isso permite validar o fluxo de recuperação de senha de ponta a ponta sem
 * configuração externa: o link de redefinição fica disponível no arquivo para o
 * teste empírico.
 */
@Service
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "log", matchIfMissing = true)
public class DevCaptureMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(DevCaptureMailService.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path captureFile;

    public DevCaptureMailService(
            @Value("${app.password-reset.dev-capture-file:data/password-reset-links.log}") String captureFile) {
        this.captureFile = Paths.get(captureFile).toAbsolutePath();
    }

    @Override
    public void sendPasswordResetEmail(String to, String userName, String resetLink) {
        String entry = String.format(
                "[%s] RECUPERAÇÃO DE SENHA%nPara: %s (%s)%nAssunto: Redefinição de senha - RevisaLab%nLink: %s%n",
                LocalDateTime.now().format(TS), to, userName, resetLink);

        // Sempre visível no log da aplicação (fácil de localizar durante o dev).
        log.info("Captura de e-mail (dev):\n{}", entry);

        try {
            Files.createDirectories(captureFile.getParent());
            Files.writeString(captureFile, entry + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("Não foi possível gravar o e-mail de recuperação no arquivo de captura {}: {}",
                    captureFile, e.getMessage());
        }
    }
}
