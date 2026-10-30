package com.revisalab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representação simplificada de uma trilha
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrailDTO {
    private Long id;
    private String title;
    private String description;
    private String category;
    private Integer difficulty;
    private String icon;
    private String color;
    private Integer totalModules;
    private Integer totalExercises;
    private Integer estimatedHours;
    private Double progress;
    private Integer completedExercises;
}