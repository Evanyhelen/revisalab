package com.revisalab.controller;

import com.revisalab.service.EnemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Expõe questões do ENEM (API pública enem.dev) para prática.
 * As questões são servidas em modo leitura (a validação é feita no cliente),
 * mantendo o fluxo de XP persistido para os exercícios do banco local.
 */
@RestController
@RequestMapping("/exercises/enem")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
@RequiredArgsConstructor
public class EnemController {

    private final EnemService enemService;

    @GetMapping("/years")
    public ResponseEntity<?> getYears() {
        return ResponseEntity.ok(enemService.getYears());
    }

    @GetMapping("/{discipline}")
    public ResponseEntity<?> getQuestions(@PathVariable String discipline,
                                          @RequestParam(required = false) Integer year,
                                          @RequestParam(defaultValue = "10") int limit) {
        if (limit < 1 || limit > 20) {
            return ResponseEntity.badRequest().body(Map.of("error", "limit deve estar entre 1 e 20"));
        }
        try {
            List<Map<String, Object>> questions = enemService.getQuestions(discipline, year, limit);
            return ResponseEntity.ok(questions);
        } catch (Exception e) {
            return ResponseEntity.status(502)
                    .body(Map.of("error", "Falha ao consultar a API do ENEM", "details", e.getMessage()));
        }
    }
}
