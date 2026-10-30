package com.revisalab.mapper;

import com.revisalab.dto.response.ExerciseDTO;
import com.revisalab.dto.response.ExerciseOptionDTO;
import com.revisalab.model.AnswerOption;
import com.revisalab.model.Exercise;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Mapper para conversão de Exercise (entidade) para ExerciseDTO (contrato do frontend).
 */
@Component
public class ExerciseMapper {

    public ExerciseDTO toDTO(Exercise exercise) {
        String category = exercise.getModule() != null && exercise.getModule().getTrail() != null
                ? exercise.getModule().getTrail().getCategory()
                : null;

        List<ExerciseOptionDTO> options = exercise.getAnswerOptions().stream()
                .sorted(Comparator.comparing(AnswerOption::getDisplayOrder))
                .map(option -> ExerciseOptionDTO.builder()
                        .id(letterFor(option.getDisplayOrder()))
                        .text(option.getText())
                        .build())
                .toList();

        return ExerciseDTO.builder()
                .id(exercise.getId())
                .question(exercise.getQuestion())
                .type(exercise.getType())
                .options(options)
                .explanation(exercise.getExplanation())
                .difficulty(exercise.getDifficulty())
                .category(category)
                .xpReward(exercise.getXpReward())
                .build();
    }

    public List<ExerciseDTO> toDTOList(List<Exercise> exercises) {
        return exercises.stream().map(this::toDTO).toList();
    }

    private String letterFor(Integer displayOrder) {
        if (displayOrder == null) {
            return "A";
        }
        return String.valueOf((char) ('A' + displayOrder));
    }
}
