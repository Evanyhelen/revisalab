package com.revisalab.repository;

import com.revisalab.model.AnswerOption;
import com.revisalab.model.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerOptionRepository extends JpaRepository<AnswerOption, Long> {
    List<AnswerOption> findByExercise(Exercise exercise);
    List<AnswerOption> findByExerciseOrderByDisplayOrder(Exercise exercise);
    void deleteByExercise(Exercise exercise);
}
