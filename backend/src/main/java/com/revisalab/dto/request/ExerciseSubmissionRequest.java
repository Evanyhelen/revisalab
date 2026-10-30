package com.revisalab.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de submissão de resposta de exercício.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseSubmissionRequest {
    private Long exerciseId;
    private String answer;
    private Integer timeSpent;
}
