package com.revisalab.controller;

import com.revisalab.dto.request.ForgotPasswordRequest;
import com.revisalab.dto.request.LoginRequest;
import com.revisalab.dto.request.ResetPasswordRequest;
import com.revisalab.dto.request.SignupRequest;
import com.revisalab.mapper.AuthMapper;
import com.revisalab.model.User;
import com.revisalab.repository.UserRepository;
import com.revisalab.security.JwtUtils;
import com.revisalab.security.UserDetailsImpl;
import com.revisalab.service.AuthService;
import com.revisalab.service.PasswordRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final AuthMapper authMapper;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordRecoveryService passwordRecoveryService;

    /**
     * Endpoint de login - retorna o token JWT.
     */
    @PostMapping("/signin")
    public ResponseEntity<?> signin(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String jwt = jwtUtils.generateJwtToken(authentication);

            User user = userRepository.findByEmail(userDetails.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return ResponseEntity.ok(authMapper.toJwtResponse(user, jwt));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "Credenciais inválidas",
                    "message", "Email ou senha incorretos"
            ));
        }
    }

    /**
     * Endpoint de teste (não requer autenticação).
     */
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(Map.of(
                "message", "API funcionando!",
                "status", "OK",
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Endpoint de registro de novo usuário.
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest signupRequest) {
        try {
            // Retorna 409 para email duplicado (o front trata essa situação com uma
            // mensagem amigável específica, em vez do erro genérico 400).
            if (!authService.isEmailAvailable(signupRequest.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "error", "Falha no registro",
                        "message", "Este email já está em uso"
                ));
            }

            User user = authService.register(signupRequest);

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(signupRequest.getEmail(), signupRequest.getPassword())
            );

            String jwt = jwtUtils.generateJwtToken(authentication);

            return ResponseEntity.ok(authMapper.toJwtResponse(user, jwt));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Falha no registro",
                    "message", e.getMessage()
            ));
        } catch (AuthenticationException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Credenciais inválidas",
                    "message", "Erro ao autenticar após registro"
            ));
        }
    }

    /**
     * Endpoint público de health check.
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    /**
     * Verifica se um email está disponível para registro.
     */
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(Map.of("available", authService.isEmailAvailable(email)));
    }

    /**
     * Solicita a recuperação de senha. Retorna sempre a mesma mensagem, exista ou
     * não uma conta com o e-mail informado, para não revelar se o e-mail está
     * cadastrado (evita enumeração de contas).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordRecoveryService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(Map.of(
                "message", "Se um e-mail estiver cadastrado, enviaremos as instruções de recuperação."
        ));
    }

    /**
     * Define uma nova senha a partir de um token de recuperação válido.
     * Retorna 400 para links inválidos, expirados ou já utilizados.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordRecoveryService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of(
                    "message", "Senha redefinida com sucesso. Você já pode entrar com a nova senha."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Falha na redefinição de senha",
                    "message", e.getMessage()
            ));
        }
    }
}
