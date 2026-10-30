package com.revisalab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resultado de validação de exercício
 * Encapsula os dados retornados ao cliente
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseValidationResult {
    private boolean correct;
    private String explanation;
    private String correctAnswer;
    private int xpEarned;
    private int userXp;
    private int userLevel;
    private int userStreak;
}
