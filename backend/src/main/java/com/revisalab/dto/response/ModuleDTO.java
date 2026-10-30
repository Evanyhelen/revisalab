package com.revisalab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representação de um módulo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleDTO {
    private Long id;
    private String title;
    private String description;
    private Integer orderNumber;
    private Integer totalExercises;
    private Integer estimatedMinutes;
    private Boolean completed;
}