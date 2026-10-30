package com.revisalab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para uma alternativa de exercício (ex.: A, B, C, D).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseOptionDTO {
    private String id;
    private String text;
}
