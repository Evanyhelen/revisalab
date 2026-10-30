package com.revisalab.service;

import com.revisalab.model.PasswordResetToken;
import com.revisalab.model.User;
import com.revisalab.repository.PasswordResetTokenRepository;
import com.revisalab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Fluxo de recuperação de senha.
 *
 * - {@link #requestPasswordReset} nunca revela se o e-mail possui conta
 *   (resposta idêntica para e-mails existentes e inexistentes).
 * - Os tokens são aleatórios, armazenados apenas como hash SHA-256, expiram e
 *   são de uso único. Gerar um novo token invalida os anteriores.
 * - A nova senha é persistida com BCrypt (mesmo {@code PasswordEncoder} do login).
 */
@Service
@Transactional
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Value("${app.password-reset.token-ttl-minutes:30}")
    private long tokenTtlMinutes;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public PasswordRecoveryService(UserRepository userRepository,
                                   PasswordResetTokenRepository tokenRepository,
                                   PasswordEncoder passwordEncoder,
                                   MailService mailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    /**
     * Gera e "envia" o link de recuperação. A resposta ao usuário final é sempre
     * a mesma, independentemente de o e-mail existir ou não.
     */
    public void requestPasswordReset(String email) {
        String normalized = email == null ? "" : email.toLowerCase().trim();
        Optional<User> userOpt = userRepository.findByEmail(normalized);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Um novo pedido invalida qualquer link anterior ainda válido.
            tokenRepository.deleteByUserId(user.getId());

            String rawToken = UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", "");
            PasswordResetToken token = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(sha256Hex(rawToken))
                    .expiresAt(LocalDateTime.now().plusMinutes(tokenTtlMinutes))
                    .used(false)
                    .build();
            tokenRepository.save(token);

            String resetLink = frontendUrl + "/auth/reset-password?token=" + rawToken;
            mailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetLink);
        }
        // Sem else: não fazemos nada para e-mails inexistentes, mas o controlador
        // retorna a mesma mensagem de sucesso, evitando enumeração de contas.
    }

    /**
     * Aplica a nova senha a partir de um token válido, não expirado e não usado.
     *
     * @throws IllegalArgumentException se o token for inválido/expirado/consumido
     *                                  ou se a nova senha for curta demais
     */
    public void resetPassword(String rawToken, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("A nova senha deve ter pelo menos 6 caracteres.");
        }

        PasswordResetToken token = tokenRepository.findByTokenHash(sha256Hex(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("Link de recuperação inválido ou expirado."));

        if (Boolean.TRUE.equals(token.getUsed()) || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Link de recuperação inválido ou expirado.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Marca como usado e remove todos os tokens restantes do usuário.
        token.setUsed(true);
        tokenRepository.save(token);
        tokenRepository.deleteByUserId(user.getId());
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível", e);
        }
    }
}
