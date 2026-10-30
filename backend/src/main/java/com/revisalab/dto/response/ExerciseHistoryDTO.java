package com.revisalab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de item do histórico de exercícios do usuário.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseHistoryDTO {
    private Long id;
    private String question;
    private String category;
    private boolean correct;
    private LocalDateTime timestamp;
    private int xpEarned;

    /** Verdadeiro quando o item representa uma sessão (ENEM/Construtor), não uma questão individual. */
    private Boolean isSession;

    /** Acertos da sessão (apenas quando {@link #isSession}). */
    private int correctAnswers;

    /** Total de questões da sessão (apenas quando {@link #isSession}). */
    private int totalQuestions;
}
