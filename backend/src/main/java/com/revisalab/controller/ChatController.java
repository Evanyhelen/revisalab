package com.revisalab.controller;

import com.revisalab.dto.request.ChatRequest;
import com.revisalab.dto.response.ChatMessageDTO;
import com.revisalab.dto.response.ChatResponse;
import com.revisalab.model.User;
import com.revisalab.repository.UserRepository;
import com.revisalab.security.UserDetailsImpl;
import com.revisalab.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller do Chat de IA.
 * Responsabilidades: receber a mensagem, validar entrada e delegar ao serviço.
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final UserRepository userRepository;
    private final ChatService chatService;

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestBody ChatRequest request,
                                         Authentication authentication) {
        String question = request.getQuestion();
        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Pergunta não pode ser vazia"
            ));
        }

        question = question.trim();
        if (question.length() > 500) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Pergunta muito longa (máximo 500 caracteres)"
            ));
        }

        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            ChatResponse chatResponse = chatService.askQuestion(user, question, "general");
            return ResponseEntity.ok(chatResponse);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "success", false,
                    "error", "Serviço de IA temporariamente indisponível",
                    "details", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "success", false,
                    "error", "Não foi possível obter resposta da IA no momento. Tente novamente.",
                    "details", e.getMessage()
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "Chat service is running",
                "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            List<ChatMessageDTO> history = chatService.getHistory(user);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Não foi possível carregar o histórico do chat."
            ));
        }
    }

    @DeleteMapping("/history")
    public ResponseEntity<?> clearHistory(Authentication authentication) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
            chatService.clearHistory(user);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "error", "Não foi possível limpar o histórico do chat."
            ));
        }
    }
}
