package com.revisalab.controller;

import com.revisalab.security.UserDetailsImpl;
import com.revisalab.service.BaseBuilderService;
import com.revisalab.service.EnemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Expõe o Construtor de Base (RF10) para o frontend.
 * O jogo roda no cliente (validação local), e o backend persiste o XP
 * conquistado ao final da sessão.
 */
@RestController
@RequestMapping("/builder")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
@RequiredArgsConstructor
public class BuilderController {

    private final BaseBuilderService baseBuilderService;
    private final EnemService enemService;

    /**
     * Fornece as questões do Construtor de Base. O conteúdo vem da API pública do
     * ENEM, misturando disciplinas e sortendo um ano a cada chamada — assim, ao
     * "jogar de novo", novas perguntas aparecem.
     */
    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions(@RequestParam(defaultValue = "8") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        List<Map<String, Object>> questions = enemService.getMixedQuestions(safeLimit);
        // Remove o gabarito (correctAnswer) e a explicação (que embute o gabarito)
        // da listagem, para não vazar a resposta antes do usuário responder. O
        // gabarito volta somente no POST /builder/submit, após a tentativa.
        List<Map<String, Object>> sanitized = questions.stream()
                .map(q -> {
                    Map<String, Object> copy = new LinkedHashMap<>(q);
                    copy.remove("correctAnswer");
                    copy.remove("explanation");
                    return copy;
                })
                .toList();
        return ResponseEntity.ok(sanitized);
    }

    /**
     * Valida no servidor a resposta de uma questão do Construtor de Base. Recebe o
     * id da questão (o mesmo devolvido por /builder/questions) e a alternativa
     * escolhida (letra). O gabarito e a explicação só são devolvidos aqui, após a
     * tentativa, sem expor a resposta na listagem de questões.
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitAnswer(@RequestBody Map<String, Object> body) {
        long questionId = body.get("questionId") instanceof Number n ? n.longValue() : 0L;
        String answer = body.get("answer") instanceof String s ? s : null;

        if (questionId <= 0 || answer == null || answer.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Requisição inválida",
                    "message", "questionId e answer são obrigatórios"
            ));
        }

        Map<String, String> resolved = enemService.resolveCorrectAnswer(questionId);
        if (resolved == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Questão não encontrada",
                    "message", "Não foi possível validar esta questão"
            ));
        }

        String correct = resolved.get("correctAnswer");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("correct", correct != null && correct.equalsIgnoreCase(answer.trim()));
        result.put("correctAnswer", correct);
        result.put("explanation", resolved.get("explanation"));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/complete")
    public ResponseEntity<?> completeSession(@RequestBody Map<String, Object> body,
                                             Authentication authentication) {
        int correctAnswers = body.get("correctAnswers") instanceof Number n ? n.intValue() : 0;
        int totalQuestions = body.get("totalQuestions") instanceof Number n ? n.intValue() : 0;
        int durationSeconds = body.get("durationSeconds") instanceof Number n ? n.intValue() : 0;
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(baseBuilderService.completeSession(
                userDetails.getId(), correctAnswers, totalQuestions, durationSeconds));
    }
}
