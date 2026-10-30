package com.revisalab.controller;

import com.revisalab.dto.request.ExerciseSubmissionRequest;
import com.revisalab.dto.response.ExerciseDTO;
import com.revisalab.dto.response.ExerciseHistoryDTO;
import com.revisalab.dto.response.ExerciseStatsDTO;
import com.revisalab.dto.response.ExerciseValidationResult;
import com.revisalab.security.UserDetailsImpl;
import com.revisalab.service.ExerciseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller de Exercícios.
 * Responsabilidades: receber requisições, delegar ao serviço e devolver respostas.
 */
@RestController
@RequestMapping("/exercises")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;

    @GetMapping("/by-trail/{trailId}")
    public ResponseEntity<List<ExerciseDTO>> getExercisesByTrail(@PathVariable Long trailId) {
        return ResponseEntity.ok(exerciseService.getExercisesByTrail(trailId));
    }

    @GetMapping("/by-category/{category}")
    public ResponseEntity<List<ExerciseDTO>> getExercisesByCategory(@PathVariable String category,
                                                                    @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(exerciseService.getExercisesByCategory(category, limit));
    }

    @GetMapping("/random")
    public ResponseEntity<List<ExerciseDTO>> getRandomExercises(@RequestParam(defaultValue = "5") int count) {
        return ResponseEntity.ok(exerciseService.getRandomExercises(count));
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitExerciseAnswer(@RequestBody ExerciseSubmissionRequest submission,
                                                  Authentication authentication) {
        if (submission.getExerciseId() == null || submission.getAnswer() == null) {
            return ResponseEntity.badRequest().body("exerciseId e answer são obrigatórios");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        ExerciseValidationResult result = exerciseService.submitAnswer(
                submission.getExerciseId(), submission.getAnswer(), userDetails.getId(),
                submission.getTimeSpent());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/complete-session")
    public ResponseEntity<?> completeSession(@RequestBody Map<String, Object> body,
                                             Authentication authentication) {
        int xp = body != null && body.get("xp") instanceof Number n ? n.intValue() : 0;
        int correctAnswers = body != null && body.get("correctAnswers") instanceof Number n ? n.intValue() : 0;
        int totalQuestions = body != null && body.get("totalQuestions") instanceof Number n ? n.intValue() : 0;
        String category = body != null && body.get("category") instanceof String s ? s : null;
        int durationSeconds = body != null && body.get("durationSeconds") instanceof Number n ? n.intValue() : 0;
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(exerciseService.completeSession(
                userDetails.getId(), xp, correctAnswers, totalQuestions, category, durationSeconds));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ExerciseHistoryDTO>> getExerciseHistory(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(exerciseService.getHistory(userDetails.getId()));
    }

    @GetMapping("/stats")
    public ResponseEntity<ExerciseStatsDTO> getExerciseStats(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(exerciseService.getStats(userDetails.getId()));
    }
}
