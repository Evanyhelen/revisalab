package com.revisalab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO de estatísticas de exercícios do usuário.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseStatsDTO {
    private long totalExercises;
    private long correctAnswers;
    private long incorrectAnswers;
    private double accuracyRate;
    private int totalXpEarned;
    private int averageTimePerExercise;
    private Map<String, CategoryStat> categoryStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStat {
        private long total;
        private long correct;
        private double accuracy;
        private int averageTime;
    }
}
