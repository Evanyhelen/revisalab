package com.revisalab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de exercício no formato consumido pelo frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseDTO {
    private Long id;
    private String question;
    private String type;
    private List<ExerciseOptionDTO> options;
    private String explanation;
    private Integer difficulty;
    private String category;
    private Integer xpReward;
}
